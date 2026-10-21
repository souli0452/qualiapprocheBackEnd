package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcTokenDto;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.userservice.client.StructureClient;
import com.qualiapproche.userservice.config.WebClientConfig.ReponseKeycloakEnErreur;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.JetonDeCompte;
import com.qualiapproche.userservice.entities.mappers.KcUserMapper;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Comptes et mots de passe, côté service : ce que Keycloak reçoit, et ce qui lui est refusé.
 *
 * <p>Keycloak est simulé ; la garde des habilitations est réelle, sur des affectations simulées.</p>
 */
class KcUserServiceSecuriteTest {

    private static final String MOI = "moi";
    private static final String CIBLE = "cible";

    private final KcTokenService jetonsKeycloak = mock(KcTokenService.class);
    private final Keycloak keycloak = mock(Keycloak.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    private final UsersResource comptes = mock(UsersResource.class);
    private final UserResource compteCible = mock(UserResource.class);
    private final AppRoleRepository roles = mock(AppRoleRepository.class);
    private final UserRoleAssignmentRepository affectations = mock(UserRoleAssignmentRepository.class);
    private final PlafondDUtilisateurs plafond = mock(PlafondDUtilisateurs.class);
    private final JetonsDeCompte jetons = mock(JetonsDeCompte.class);
    private KcUserService service;

    @BeforeEach
    void preparer() {
        KcAuthProperties proprietes = new KcAuthProperties();
        proprietes.setRealm("quali");
        GardeDesHabilitations garde = new GardeDesHabilitations(affectations,
                new com.qualiapproche.common.config.PermissionChecker());
        service = new KcUserService(jetonsKeycloak, mock(KcUserMapper.class), keycloak, proprietes, roles,
                affectations, mock(StructureClient.class), mock(SendMailService.class), plafond,
                mock(ProfilPersonnel.class), jetons, garde);
        when(keycloak.realm("quali").users()).thenReturn(comptes);
        when(comptes.get(CIBLE)).thenReturn(compteCible);
        when(affectations.findByUserId(anyString())).thenReturn(List.of());
        connecter("gestionnaire");
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String nom) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "RS256").subject(MOI)
                .claim("preferred_username", nom)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @Test
    @DisplayName("Après un changement de mot de passe, les sessions du compte sont fermées")
    void changement_fermeLesSessions() {
        service.updatePassword(CIBLE, "Nouveau-1");

        ArgumentCaptor<CredentialRepresentation> credential = ArgumentCaptor.forClass(CredentialRepresentation.class);
        var ordre = inOrder(compteCible);
        ordre.verify(compteCible).resetPassword(credential.capture());
        ordre.verify(compteCible).logout();
        assertThat(credential.getValue().getValue()).isEqualTo("Nouveau-1");
        assertThat(credential.getValue().isTemporary()).isFalse();
    }

    @Test
    @DisplayName("Une déconnexion qui échoue ne défait pas le changement de mot de passe")
    void deconnexionEnEchec_changementMaintenu() {
        doThrow(new RuntimeException("Keycloak injoignable")).when(compteCible).logout();

        assertThatCode(() -> service.updatePassword(CIBLE, "Nouveau-1")).doesNotThrowAnyException();
        verify(compteCible).resetPassword(any());
    }

    @Test
    @DisplayName("Un mot de passe refusé par la politique de Keycloak : 400, et non 500")
    void politiqueDeMotDePasse() {
        doThrow(new jakarta.ws.rs.BadRequestException()).when(compteCible).resetPassword(any());

        assertThatThrownBy(() -> service.updatePassword(CIBLE, "court"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(compteCible, never()).logout();
    }

    @Test
    @DisplayName("Réinitialisation par lien : le jeton d'abord, puis le mot de passe, puis les sessions")
    void parLien() {
        service.reinitializePwd(CIBLE, "Nouveau-1", "jeton");

        var ordre = inOrder(jetons, compteCible);
        ordre.verify(jetons).consommer(CIBLE, JetonDeCompte.Usage.REINITIALISATION, "jeton");
        ordre.verify(compteCible).resetPassword(any());
        ordre.verify(compteCible).logout();
    }

    @Test
    @DisplayName("Un gestionnaire ne fixe pas le mot de passe d'un compte plus privilégié")
    void parGestionnaire_comptePlusPrivilegie() {
        AppRole superAdmin = new AppRole();
        superAdmin.setName("SUPER_ADMIN");
        when(affectations.findByUserId(CIBLE)).thenReturn(List.of(
                com.qualiapproche.userservice.entities.UserRoleAssignment.builder().userId(CIBLE).role(superAdmin).build()));

        assertThatThrownBy(() -> service.fixerMotDePasseParGestionnaire(CIBLE, "Nouveau-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(compteCible, never()).resetPassword(any());
    }

    @Test
    @DisplayName("Un gestionnaire ne désactive pas un compte plus privilégié")
    void statut_comptePlusPrivilegie() {
        AppRole licence = new AppRole();
        licence.setName("LICENCE");
        licence.setPermissions(new java.util.ArrayList<>(List.of("licence-write")));
        when(affectations.findByUserId(CIBLE)).thenReturn(List.of(
                com.qualiapproche.userservice.entities.UserRoleAssignment.builder().userId(CIBLE).role(licence).build()));

        assertThatThrownBy(() -> service.changeUserStatus(CIBLE, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactiver ce compte");
        verify(compteCible, never()).update(any());
    }

    @Test
    @DisplayName("Changer ses propres rôles par la fiche d'administration : refusé")
    void fiche_sesPropresRoles() {
        AppRole lecteur = new AppRole();
        lecteur.setName("LECTEUR");
        when(roles.findByName("LECTEUR")).thenReturn(Optional.of(lecteur));
        KcUserDto fiche = KcUserDto.builder().id(MOI).roles(List.of("LECTEUR")).build();

        assertThatThrownBy(() -> service.updateUser(fiche))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propre compte");
        verify(affectations, never()).delete(any());
    }

    @Test
    @DisplayName("Un nom commençant par « service-account- » est refusé avant toute création")
    void nomDeCompteDeService() {
        KcUserDto nouveau = KcUserDto.builder().username("Service-Account-referentiel").build();

        assertThatThrownBy(() -> service.createUser(nouveau))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(comptes, never()).create(any());
        verifyNoInteractions(plafond);
    }

    @Test
    @DisplayName("Créer un compte avec un rôle que le demandeur ne couvre pas : refusé avant Keycloak")
    void creation_roleNonCouvert() {
        AppRole licence = new AppRole();
        licence.setName("LICENCE");
        licence.setPermissions(new java.util.ArrayList<>(List.of("licence-write")));
        when(roles.findByName("LICENCE")).thenReturn(Optional.of(licence));
        KcUserDto nouveau = KcUserDto.builder().username("b.kabore").roles(List.of("LICENCE")).build();

        assertThatThrownBy(() -> service.createUser(nouveau)).isInstanceOf(BusinessException.class);
        verify(comptes, never()).create(any());
    }

    @Test
    @DisplayName("Identifiants refusés par Keycloak : 401 uniforme, et non 500")
    void connexion_identifiantsFaux() {
        when(jetonsKeycloak.getAccessToken(any())).thenThrow(new ReponseKeycloakEnErreur(401, "{\"error\":\"invalid_grant\"}"));

        assertThatThrownBy(() -> service.login(KcLoginRequestDto.builder().username("a").password("faux").build(),
                new MockHttpServletResponse()))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).isEqualTo("Identifiant ou mot de passe incorrect.");
                });
    }

    @Test
    @DisplayName("Keycloak en panne à la connexion : l'erreur reste une erreur du serveur")
    void connexion_keycloakEnPanne() {
        when(jetonsKeycloak.getAccessToken(any())).thenThrow(new ReponseKeycloakEnErreur(503, ""));

        assertThatThrownBy(() -> service.login(KcLoginRequestDto.builder().username("a").password("b").build(),
                new MockHttpServletResponse()))
                .isInstanceOf(ReponseKeycloakEnErreur.class);
    }

    @Test
    @DisplayName("Renouvellement sans cookie, ou refusé par Keycloak : 401")
    void renouvellement() {
        MockHttpServletRequest sansCookie = new MockHttpServletRequest();
        assertThatThrownBy(() -> service.refreshToken(sansCookie, new MockHttpServletResponse()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        MockHttpServletRequest avecCookie = new MockHttpServletRequest();
        avecCookie.setCookies(new Cookie("refresh_token", "expire"));
        when(jetonsKeycloak.getRefreshToken("expire")).thenThrow(new ReponseKeycloakEnErreur(400, "invalid_grant"));
        assertThatThrownBy(() -> service.refreshToken(avecCookie, new MockHttpServletResponse()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Changement de son mot de passe : ancien vérifié, nouveau fixé, sessions fermées")
    void sonMotDePasse() {
        when(jetonsKeycloak.getAccessToken(any())).thenReturn(new KcTokenDto());
        UserRepresentation compte = new UserRepresentation();
        compte.setId(CIBLE);
        when(comptes.search("a.traore", true)).thenReturn(List.of(compte));

        ResponseEntity<Object> reponse = service.updatePassword("a.traore", "Ancien-1", "Nouveau-1");

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(compteCible).resetPassword(any());
        verify(compteCible).logout();
    }

    @Test
    @DisplayName("Changement de son mot de passe : ancien faux, 401 et rien n'est écrit")
    void sonMotDePasse_ancienFaux() {
        when(jetonsKeycloak.getAccessToken(any())).thenThrow(new ReponseKeycloakEnErreur(401, "invalid_grant"));

        ResponseEntity<Object> reponse = service.updatePassword("a.traore", "faux", "Nouveau-1");

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Changement de mot de passe d'un autre compte : un gestionnaire ne vise pas un SUPER_ADMIN")
    void motDePasseDUnAutre_superAdmin() {
        when(jetonsKeycloak.getAccessToken(any())).thenReturn(new KcTokenDto());
        UserRepresentation compte = new UserRepresentation();
        compte.setId(CIBLE);
        when(comptes.search("admin", true)).thenReturn(List.of(compte));
        AppRole superAdmin = new AppRole();
        superAdmin.setName("SUPER_ADMIN");
        when(affectations.findByUserId(CIBLE)).thenReturn(List.of(
                com.qualiapproche.userservice.entities.UserRoleAssignment.builder().userId(CIBLE).role(superAdmin).build()));

        assertThatThrownBy(() -> service.updatePassword("admin", "Ancien-1", "Nouveau-1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getMessage()).contains("changer son mot de passe");
                });
        // Refusé avant toute ouverture de session : l'ancien mot de passe n'est pas mis à l'épreuve.
        verify(jetonsKeycloak, never()).getAccessToken(any());
        verify(compteCible, never()).resetPassword(any());
        verify(compteCible, never()).logout();
    }

    @Test
    @DisplayName("Changement de son propre mot de passe : le titulaire passe, même SUPER_ADMIN")
    void sonMotDePasse_titulaireSuperAdmin() {
        when(jetonsKeycloak.getAccessToken(any())).thenReturn(new KcTokenDto());
        UserRepresentation compte = new UserRepresentation();
        compte.setId(MOI);
        UserResource monCompte = mock(UserResource.class);
        when(comptes.get(MOI)).thenReturn(monCompte);
        when(comptes.search("moi", true)).thenReturn(List.of(compte));
        AppRole superAdmin = new AppRole();
        superAdmin.setName("SUPER_ADMIN");
        when(affectations.findByUserId(MOI)).thenReturn(List.of(
                com.qualiapproche.userservice.entities.UserRoleAssignment.builder().userId(MOI).role(superAdmin).build()));

        ResponseEntity<Object> reponse = service.updatePassword("moi", "Ancien-1", "Nouveau-1");

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(monCompte).resetPassword(any());
    }

    @Test
    @DisplayName("Le profil se lit sur le jeton vérifié, plus sur un cookie décodé à la main")
    void profil_identiteDuJeton() {
        UserRepresentation moi = new UserRepresentation();
        moi.setId(MOI);
        UserResource monCompte = mock(UserResource.class);
        when(comptes.get(MOI)).thenReturn(monCompte);
        when(monCompte.toRepresentation()).thenReturn(moi);

        assertThat(service.getMe()).containsEntry("userId", MOI);

        SecurityContextHolder.clearContext();
        assertThatThrownBy(service::getMe).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
