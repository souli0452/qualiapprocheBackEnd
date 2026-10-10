package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.auth.KcRoleDto;
import com.qualiapproche.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rôles du realm Keycloak : ceux qui ouvriraient une permission ne se créent ni ne s'attribuent.
 *
 * <p>{@code PermissionChecker} croit les rôles du jeton : un rôle Keycloak nommé comme une
 * permission la donnait à qui le portait, sans passer par les rôles applicatifs.</p>
 */
class RolesKeycloakReservesTest {

    private static final Set<String> DICTIONNAIRE = Set.of("MANAGE_USER", "ROLE_MANAGE", "nc-read");

    private final Keycloak keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);
    private final AppRoleService appRoleService = mock(AppRoleService.class);
    private final RoleScopeResource rolesDuCompte = mock(RoleScopeResource.class);
    /** Simulée, elle laisse passer : le demandeur est SUPER_ADMIN, sauf dans les tests qui le refusent. */
    private final GardeDesHabilitations garde = mock(GardeDesHabilitations.class);
    private KcRoleService service;

    @BeforeEach
    void preparer() {
        service = new KcRoleService(keycloak, null, null, null, null, appRoleService, garde);
        ReflectionTestUtils.setField(service, "realm", "quali");
        when(appRoleService.permissionsDuDictionnaire()).thenReturn(DICTIONNAIRE);
        when(keycloak.realm("quali").users().get(any()).roles().realmLevel()).thenReturn(rolesDuCompte);
        when(rolesDuCompte.listAll()).thenReturn(List.of());
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "RS256").subject("moi")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sont réservés : l'administration, le service technique, et toute forme d'une permission")
    void nomsReserves() {
        assertThat(KcRoleService.nomReserve("SUPER_ADMIN", DICTIONNAIRE)).isTrue();
        assertThat(KcRoleService.nomReserve("superadmin", DICTIONNAIRE)).isTrue();
        assertThat(KcRoleService.nomReserve("SERVICE_TECHNIQUE", DICTIONNAIRE)).isTrue();
        assertThat(KcRoleService.nomReserve("manage_user", DICTIONNAIRE)).isTrue();
        assertThat(KcRoleService.nomReserve("NC-READ", DICTIONNAIRE)).isTrue();
        // Le jeton présente « ROLE_<nom> » : « MANAGE » y devient ROLE_MANAGE, qui est une permission.
        assertThat(KcRoleService.nomReserve("MANAGE", DICTIONNAIRE)).isTrue();
        assertThat(KcRoleService.nomReserve("ROLE_MANAGE", DICTIONNAIRE)).isTrue();
    }

    @Test
    @DisplayName("Un nom de métier n'est pas réservé (son écriture reste due au seul SUPER_ADMIN)")
    void nomsLibres() {
        assertThat(KcRoleService.nomReserve("AUDITEUR", DICTIONNAIRE)).isFalse();
        assertThat(KcRoleService.nomReserve("offline_access", DICTIONNAIRE)).isFalse();
    }

    @Test
    @DisplayName("Créer un rôle Keycloak au nom réservé : 403, rien n'est créé")
    void creation_refusee() {
        assertThatThrownBy(() -> service.createRole(KcRoleDto.builder().name("MANAGE_USER").build()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(keycloak.realm("quali").roles(), never()).create(any());
    }

    @Test
    @DisplayName("Attribuer un rôle Keycloak au nom réservé : 403, rien n'est attribué")
    void attribution_refusee() {
        assertThatThrownBy(() -> service.assignRoles("autre", List.of("SUPER_ADMIN")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(rolesDuCompte, never()).add(anyList());
    }

    @Test
    @DisplayName("Modifier ses propres rôles Keycloak : 403")
    void sesPropresRoles_refuse() {
        assertThatThrownBy(() -> service.assignRoles("moi", List.of("AUDITEUR")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propre compte");
        verify(rolesDuCompte, never()).add(anyList());
    }

    @Test
    @DisplayName("Un rôle de métier s'attribue à autrui")
    void attribution_permise() {
        RoleRepresentation auditeur = new RoleRepresentation();
        auditeur.setName("AUDITEUR");
        when(keycloak.realm("quali").roles().get("AUDITEUR").toRepresentation()).thenReturn(auditeur);

        service.assignRoles("autre", List.of("AUDITEUR"));

        verify(rolesDuCompte).add(List.of(auditeur));
    }

    @Test
    @DisplayName("Sans SUPER_ADMIN, aucune écriture ne parvient à Keycloak, même pour un nom de métier")
    void sansSuperAdmin_rienNEstEcrit() {
        BusinessException refus = new BusinessException("Seul un SUPER_ADMIN peut …", HttpStatus.FORBIDDEN);
        doThrow(refus).when(garde).exigerSuperAdmin(anyString());

        assertThatThrownBy(() -> service.createRole(KcRoleDto.builder().name("RESPONSABLE_QUALITE").build()))
                .isSameAs(refus);
        assertThatThrownBy(() -> service.assignRoles("autre", List.of("RESPONSABLE_QUALITE"))).isSameAs(refus);
        assertThatThrownBy(() -> service.updateRole(KcRoleDto.builder().name("RESPONSABLE_QUALITE").build()))
                .isSameAs(refus);
        assertThatThrownBy(() -> service.deleteRole("RESPONSABLE_QUALITE")).isSameAs(refus);

        verify(keycloak.realm("quali").roles(), never()).create(any());
        verify(keycloak.realm("quali").roles(), never()).deleteRole(any());
        verify(rolesDuCompte, never()).add(anyList());
        verify(rolesDuCompte, never()).remove(anyList());
    }
}
