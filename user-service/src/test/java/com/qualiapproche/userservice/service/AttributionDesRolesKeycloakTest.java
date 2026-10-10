package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.auth.KcRoleDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.mappers.KcRoleMapper;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Rôles Keycloak d'un compte : ce que l'attribution retire, ajoute, ou ignore.
 *
 * <p>Les noms réservés ont leur propre test ({@link RolesKeycloakReservesTest}) ; ici, le demandeur
 * est un SUPER_ADMIN (garde simulée) et l'on vérifie que la liste demandée devient exactement celle
 * du compte, sans que Keycloak reçoive un ordre vide ou un rôle qu'il ne connaît pas.</p>
 */
class AttributionDesRolesKeycloakTest {

    private final Keycloak keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);
    private final AppRoleService appRoleService = mock(AppRoleService.class);
    private final RoleScopeResource rolesDuCompte = mock(RoleScopeResource.class);
    private final GardeDesHabilitations garde = mock(GardeDesHabilitations.class);
    private KcRoleService service;

    @BeforeEach
    void preparer() {
        service = new KcRoleService(keycloak, null, new KcRoleMapper(), null, null, appRoleService, garde);
        ReflectionTestUtils.setField(service, "realm", "quali");
        when(appRoleService.permissionsDuDictionnaire()).thenReturn(Set.of("MANAGE_USER"));
        when(keycloak.realm("quali").users().get(any()).roles().realmLevel()).thenReturn(rolesDuCompte);
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "RS256").subject("moi")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static RoleRepresentation role(String nom) {
        RoleRepresentation r = new RoleRepresentation();
        r.setName(nom);
        r.setId("id-" + nom);
        return r;
    }

    @Test
    @DisplayName("La liste demandée remplace celle du compte : l'absent est retiré, le nouveau ajouté")
    void remplacement() {
        when(rolesDuCompte.listAll()).thenReturn(List.of(role("AUDITEUR"), role("PILOTE")));
        when(keycloak.realm("quali").roles().get("LECTEUR").toRepresentation()).thenReturn(role("LECTEUR"));

        service.assignRoles("autrui", List.of("PILOTE", "LECTEUR"));

        ArgumentCaptor<List<RoleRepresentation>> retires = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<RoleRepresentation>> ajoutes = ArgumentCaptor.forClass(List.class);
        verify(rolesDuCompte).remove(retires.capture());
        verify(rolesDuCompte).add(ajoutes.capture());
        assertThat(retires.getValue()).extracting(RoleRepresentation::getName).containsExactly("AUDITEUR");
        assertThat(ajoutes.getValue()).extracting(RoleRepresentation::getName).containsExactly("LECTEUR");
    }

    @Test
    @DisplayName("Un rôle que Keycloak ne connaît pas est ignoré, sans faire échouer l'attribution")
    void roleInconnuIgnore() {
        when(rolesDuCompte.listAll()).thenReturn(List.of());
        when(keycloak.realm("quali").roles().get("FANTOME").toRepresentation()).thenThrow(new NotFoundException());

        assertThatCode(() -> service.assignRoles("autrui", List.of("FANTOME"))).doesNotThrowAnyException();
        verify(rolesDuCompte, never()).add(anyList());
        verify(rolesDuCompte, never()).remove(anyList());
    }

    @Test
    @DisplayName("Sur son propre compte, renvoyer ses rôles inchangés est permis : rien ne change")
    void sesRolesInchanges() {
        when(rolesDuCompte.listAll()).thenReturn(List.of(role("PILOTE")));

        assertThatCode(() -> service.assignRoles("moi", List.of("PILOTE"))).doesNotThrowAnyException();
        verify(rolesDuCompte, never()).add(anyList());
        verify(rolesDuCompte, never()).remove(anyList());
    }

    @Test
    @DisplayName("Sur son propre compte, en retirer un : 403")
    void sesRolesRetires() {
        when(rolesDuCompte.listAll()).thenReturn(List.of(role("PILOTE")));

        assertThatThrownBy(() -> service.assignRoles("moi", List.of()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(rolesDuCompte, never()).remove(anyList());
    }

    @Test
    @DisplayName("Sans liste de rôles : 400, avant même la garde")
    void listeAbsente() {
        assertThatThrownBy(() -> service.assignRoles("autrui", null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(garde);
    }

    @Test
    @DisplayName("Créer un rôle sans nom : 400, avant même la garde")
    void creationSansNom() {
        KcRoleDto sansNom = new KcRoleDto();
        sansNom.setName(" ");

        assertThatThrownBy(() -> service.createRole(sansNom))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(garde);
    }

    @Test
    @DisplayName("Modifier ou supprimer un rôle Keycloak passe toujours par la garde SUPER_ADMIN")
    void modificationEtSuppressionGardees() {
        KcRoleDto r = new KcRoleDto();
        r.setName("PILOTE");

        service.updateRole(r);
        service.deleteRole("PILOTE");

        verify(garde).exigerSuperAdmin("modifier un rôle Keycloak");
        verify(garde).exigerSuperAdmin("supprimer un rôle Keycloak");
        verify(keycloak.realm("quali").roles()).deleteRole("PILOTE");
    }

    @Test
    @DisplayName("La liste des rôles se pagine ; une page au-delà de la fin est vide")
    void pagination() {
        when(keycloak.realm("quali").roles().list()).thenReturn(List.of(role("A"), role("B"), role("C")));

        Page<KcRoleDto> deuxieme = service.getAllRoles(PageRequest.of(1, 2));
        Page<KcRoleDto> horsBorne = service.getAllRoles(PageRequest.of(3, 2));

        assertThat(deuxieme.getContent()).extracting(KcRoleDto::getName).containsExactly("C");
        assertThat(deuxieme.getTotalElements()).isEqualTo(3);
        assertThat(horsBorne.getContent()).isEmpty();
    }
}
