package com.qualiapproche.userservice.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
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
        service = new AppRoleService(depot);
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
        when(depot.findAllByName("PILOTE")).thenReturn(List.of());

        AppRole enregistre = service.enregistrer(role(null, "  PILOTE  "));

        assertThat(enregistre.getName()).isEqualTo("PILOTE");
        verify(depot).save(any(AppRole.class));
    }

    @Test
    @DisplayName("Deux rôles ne peuvent pas porter le même nom")
    void creation_refuseUnHomonyme() {
        when(depot.findAllByName("PILOTE")).thenReturn(List.of(role(UUID.randomUUID(), "PILOTE")));

        assertThatThrownBy(() -> service.enregistrer(role(null, "PILOTE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existe déjà");

        verify(depot, never()).save(any(AppRole.class));
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
}
