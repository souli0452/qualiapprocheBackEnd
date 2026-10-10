package com.qualiapproche.userservice.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le nom d'un rôle est son identité fonctionnelle : les étapes des circuits, le catalogue
 * d'étapes, la resynchronisation des rôles standard et la résolution des destinataires le
 * désignent tous par ce nom, jamais par son identifiant technique.
 *
 * <p>Ces tests fixent les deux règles qui le rendent tenable. Elles ne se vérifient pas à
 * l'exécution : un renommage réussissait, et ses effets — étapes sans titulaire habilité,
 * notifications sans destinataire, rôle standard recréé sous son ancien nom au démarrage suivant —
 * n'apparaissaient qu'ailleurs et plus tard.</p>
 */
class AppRoleServiceTest {

    private AppRoleRepository depot;
    private AppRoleService service;

    @BeforeEach
    void setUp() {
        depot = mock(AppRoleRepository.class);
        service = new AppRoleService(depot, mock(UserRoleAssignmentRepository.class), mock(GardeDesHabilitations.class));
        when(depot.save(any(AppRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AppRole role(UUID id, String nom) {
        AppRole role = new AppRole();
        role.setId(id);
        role.setName(nom);
        return role;
    }

    @Test
    @DisplayName("Un rôle nouveau est enregistré, nom épuré des espaces superflus")
    void creation_enregistreLeRole() {
        when(depot.findAllByNameIgnoreCase("PILOTE")).thenReturn(List.of());

        AppRole enregistre = service.enregistrer(role(null, "  PILOTE  "));

        assertThat(enregistre.getName()).isEqualTo("PILOTE");
        verify(depot).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Deux rôles ne peuvent pas porter le même nom")
    void creation_refuseUnHomonyme() {
        when(depot.findAllByNameIgnoreCase("PILOTE")).thenReturn(List.of(role(UUID.randomUUID(), "PILOTE")));

        assertThatThrownBy(() -> service.enregistrer(role(null, "PILOTE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existe déjà");

        verify(depot, never()).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Un homonyme à la casse près est refusé : « pilote » à côté de « PILOTE »")
    void creation_refuseUnHomonymeALaCassePres() {
        when(depot.findAllByNameIgnoreCase("pilote")).thenReturn(List.of(role(UUID.randomUUID(), "PILOTE")));

        assertThatThrownBy(() -> service.enregistrer(role(null, "pilote")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(depot, never()).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Un nom réservé est refusé quelle que soit sa casse : on ne se crée pas « super_admin »")
    void creation_refuseUnNomReserve() {
        for (String nom : List.of("SUPER_ADMIN", "super_admin", "SuperAdmin", "service_technique")) {
            assertThatThrownBy(() -> service.enregistrer(role(null, nom)))
                    .as(nom)
                    .isInstanceOfSatisfying(BusinessException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(e.getMessage()).contains("réservé");
                    });
        }
        verify(depot, never()).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Un rôle SUPER_ADMIN déjà en base se modifie toujours : la règle ne vise que la création")
    void modification_duSuperAdminExistant() {
        UUID id = UUID.randomUUID();
        when(depot.findById(id)).thenReturn(Optional.of(role(id, "SUPER_ADMIN")));
        AppRole soumis = role(id, "SUPER_ADMIN");
        soumis.setDescription("Accès total");

        assertThat(service.enregistrer(soumis).getDescription()).isEqualTo("Accès total");
    }

    @Test
    @DisplayName("Le nom d'un rôle enregistré ne peut pas être modifié")
    void modification_refuseLeRenommage() {
        UUID id = UUID.randomUUID();
        when(depot.findById(id)).thenReturn(Optional.of(role(id, "PILOTE")));

        assertThatThrownBy(() -> service.enregistrer(role(id, "PILOTE_QUALITE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PILOTE → PILOTE_QUALITE");

        verify(depot, never()).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Le renommage est refusé en 409, pas en erreur technique")
    void modification_refuseLeRenommageEn409() {
        UUID id = UUID.randomUUID();
        when(depot.findById(id)).thenReturn(Optional.of(role(id, "PILOTE")));

        // Le front présente le message du corps de la réponse : il doit dire quoi faire, et le
        // statut le distinguer d'une panne.
        assertThatThrownBy(() -> service.enregistrer(role(id, "AUTRE")))
                .isInstanceOf(BusinessException.class)
                .extracting(erreur -> ((BusinessException) erreur).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("À nom inchangé, description et permissions sont bien reprises")
    void modification_metAJourDescriptionEtPermissions() {
        UUID id = UUID.randomUUID();
        AppRole existant = role(id, "PILOTE");
        existant.setDescription("Ancienne description");
        existant.setPermissions(List.of("nc-read"));
        when(depot.findById(id)).thenReturn(Optional.of(existant));

        AppRole soumis = role(id, "PILOTE");
        soumis.setDescription("Réceptionne, impute et valide");
        soumis.setPermissions(List.of("nc-read", "nc-validate"));

        AppRole enregistre = service.enregistrer(soumis);

        assertThat(enregistre.getName()).isEqualTo("PILOTE");
        assertThat(enregistre.getDescription()).isEqualTo("Réceptionne, impute et valide");
        assertThat(enregistre.getPermissions()).containsExactly("nc-read", "nc-validate");
    }

    @Test
    @DisplayName("Un nom vide est refusé plutôt qu'enregistré tel quel")
    void nomVide_estRefuse() {
        assertThatThrownBy(() -> service.enregistrer(role(null, "   ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("obligatoire");

        verify(depot, never()).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Une permission absente du dictionnaire est refusée : on ne se forge pas SUPER_ADMIN")
    void permissionInconnue_refusee() {
        when(depot.findAllByNameIgnoreCase("PIRATE")).thenReturn(List.of());
        AppRole forge = role(null, "PIRATE");
        forge.setPermissions(List.of("MANAGE_USER", "SUPER_ADMIN", "SERVICE_TECHNIQUE"));

        assertThatThrownBy(() -> service.enregistrer(forge))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("SUPER_ADMIN", "SERVICE_TECHNIQUE").doesNotContain("MANAGE_USER,");
                });
        verify(depot, never()).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Des permissions du dictionnaire passent")
    void permissionsConnues_acceptees() {
        when(depot.findAllByNameIgnoreCase("ADMIN_COMPTES")).thenReturn(List.of());
        AppRole role = role(null, "ADMIN_COMPTES");
        role.setPermissions(List.of("MANAGE_USER", "ROLE_MANAGE"));

        assertThat(service.enregistrer(role).getPermissions()).containsExactly("MANAGE_USER", "ROLE_MANAGE");
    }
}
