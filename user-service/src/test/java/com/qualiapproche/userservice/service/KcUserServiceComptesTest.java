package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcTokenDto;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.common.utils.PermissionsPortee;
import com.qualiapproche.userservice.client.StructureClient;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.JetonDeCompte;
import com.qualiapproche.userservice.entities.UserRoleAssignment;
import com.qualiapproche.userservice.entities.mappers.KcUserMapper;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import jakarta.servlet.http.Cookie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
 * Comptes, côté service : ce que la connexion rend, comment un compte naît, qui reçoit les courriels
 * d'étape, et ce que la réinitialisation laisse — ou non — deviner.
 *
 * <p>La garde des habilitations est simulée ici : ses refus ont leurs propres tests
 * ({@link GardeDesHabilitationsTest}, {@link KcUserServiceSecuriteTest}). On vérifie le reste du
 * chemin, une fois la garde passée.</p>
 */
class KcUserServiceComptesTest {

    private static final String MOI = "moi";
    private static final String FRONT = "https://qualisira.test";

    private final KcTokenService jetonsKeycloak = mock(KcTokenService.class);
    private final Keycloak keycloak = mock(Keycloak.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    private final UsersResource comptes = mock(UsersResource.class);
    private final AppRoleRepository roles = mock(AppRoleRepository.class);
    private final UserRoleAssignmentRepository affectations = mock(UserRoleAssignmentRepository.class);
    private final StructureClient structures = mock(StructureClient.class);
    private final SendMailService courriels = mock(SendMailService.class);
    private final PlafondDUtilisateurs plafond = mock(PlafondDUtilisateurs.class);
    private final JetonsDeCompte jetons = mock(JetonsDeCompte.class);
    private final GardeDesHabilitations garde = mock(GardeDesHabilitations.class);
    private KcUserService service;

    @BeforeEach
    void preparer() {
        KcAuthProperties proprietes = new KcAuthProperties();
        proprietes.setRealm("quali");
        service = new KcUserService(jetonsKeycloak, new KcUserMapper(), keycloak, proprietes, roles,
                affectations, structures, courriels, plafond, mock(ProfilPersonnel.class), jetons, garde);
        ReflectionTestUtils.setField(service, "frontendUrl", FRONT);
        when(keycloak.realm("quali").users()).thenReturn(comptes);
        when(affectations.findByUserId(anyString())).thenReturn(List.of());
        when(roles.findAll()).thenReturn(List.of());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String userId) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "RS256").subject(userId)
                .claim("preferred_username", userId)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private static AppRole role(String nom, String... permissions) {
        AppRole role = new AppRole();
        role.setId(UUID.randomUUID());
        role.setName(nom);
        role.setPermissions(new ArrayList<>(List.of(permissions)));
        return role;
    }

    private static UserRoleAssignment affectation(String userId, AppRole role) {
        return UserRoleAssignment.builder().userId(userId).role(role).build();
    }

    private static UserRepresentation compte(String id, String structure, String email, Boolean actif) {
        UserRepresentation u = new UserRepresentation();
        u.setId(id);
        u.setUsername(id);
        u.setEmail(email);
        u.setFirstName("Awa");
        u.setLastName("Traoré");
        u.setEnabled(actif);
        Map<String, List<String>> attributs = new HashMap<>();
        if (structure != null) {
            attributs.put("structure", List.of(structure));
        }
        attributs.put("fonction", List.of("Pilote"));
        u.setAttributes(attributs);
        return u;
    }

    /** Branche un compte Keycloak sur {@code comptes.get(id)} et rend sa ressource. */
    private UserResource brancher(UserRepresentation u) {
        UserResource ressource = mock(UserResource.class);
        when(comptes.get(u.getId())).thenReturn(ressource);
        when(ressource.toRepresentation()).thenReturn(u);
        return ressource;
    }

    @Nested
    @DisplayName("Destinataires d'un rôle (courriels d'étape)")
    class DestinatairesDUnRole {

        @Test
        @DisplayName("Un rôle vide ne désigne personne, sans interroger la base")
        void roleVide() {
            assertThat(service.getUsersByRole(" ", null)).isEmpty();
            assertThat(service.getUsersByRole(null, null)).isEmpty();
            verifyNoInteractions(affectations);
        }

        @Test
        @DisplayName("Le rôle se résout par identifiant, sinon par nom")
        void parIdentifiantPuisParNom() {
            AppRole pilote = role("PILOTE");
            brancher(compte("u1", "S1", "u1@q.bf", true));
            when(affectations.findByRole_Id(pilote.getId())).thenReturn(List.of(affectation("u1", pilote)));
            when(affectations.findByRole_NameIgnoreCase("pilote")).thenReturn(List.of(affectation("u1", pilote)));

            assertThat(service.getUsersByRole(pilote.getId().toString(), null))
                    .extracting(DestinataireDto::getUserId).containsExactly("u1");
            assertThat(service.getUsersByRole(" pilote ", null))
                    .extracting(DestinataireDto::getUserId).containsExactly("u1");
            verify(affectations).findByRole_NameIgnoreCase("pilote");
        }

        @Test
        @DisplayName("Un identifiant que personne ne porte se retente comme un nom")
        void identifiantSansPorteur() {
            UUID inconnu = UUID.randomUUID();
            when(affectations.findByRole_Id(inconnu)).thenReturn(List.of());

            assertThat(service.getUsersByRole(inconnu.toString(), null)).isEmpty();
            verify(affectations).findByRole_NameIgnoreCase(inconnu.toString());
        }

        @Test
        @DisplayName("La structure du dossier borne les destinataires à ses membres")
        void borneeALaStructure() {
            AppRole pilote = role("PILOTE");
            brancher(compte("dedans", "S1", "a@q.bf", true));
            brancher(compte("dehors", "S2", "b@q.bf", true));
            when(affectations.findByRole_NameIgnoreCase("PILOTE"))
                    .thenReturn(List.of(affectation("dedans", pilote), affectation("dehors", pilote)));

            assertThat(service.getUsersByRole("PILOTE", " S1 "))
                    .extracting(DestinataireDto::getUserId).containsExactly("dedans");
            assertThat(service.getUsersByRole("PILOTE", null))
                    .extracting(DestinataireDto::getUserId).containsExactly("dedans", "dehors");
        }

        @Test
        @DisplayName("Un rôle à portée globale n'est pas borné à la structure, quel que soit son nom")
        void porteeGlobale() {
            AppRole coordonnateur = role("Coordonnateur qualité", PermissionsPortee.TOUTES_STRUCTURES);
            brancher(compte("u1", "S2", "a@q.bf", true));
            when(affectations.findByRole_NameIgnoreCase("Coordonnateur qualité"))
                    .thenReturn(List.of(affectation("u1", coordonnateur)));

            assertThat(service.getUsersByRole("Coordonnateur qualité", "S1"))
                    .extracting(DestinataireDto::getUserId).containsExactly("u1");
        }

        @Test
        @DisplayName("Désactivés, sans adresse ou disparus de Keycloak : écartés, sans priver les autres")
        void injoignablesEcartes() {
            AppRole pilote = role("PILOTE");
            brancher(compte("inactif", "S1", "i@q.bf", false));
            brancher(compte("muet", "S1", " ", true));
            when(comptes.get("disparu")).thenThrow(new jakarta.ws.rs.NotFoundException());
            brancher(compte("joignable", "S1", "j@q.bf", true));
            when(affectations.findByRole_NameIgnoreCase("PILOTE")).thenReturn(List.of(
                    affectation("inactif", pilote), affectation("muet", pilote),
                    affectation("disparu", pilote), affectation("joignable", pilote),
                    affectation("joignable", pilote), affectation(null, pilote)));

            List<DestinataireDto> destinataires = service.getUsersByRole("PILOTE", null);

            assertThat(destinataires).singleElement().satisfies(d -> {
                assertThat(d.getUserId()).isEqualTo("joignable");
                assertThat(d.getEmail()).isEqualTo("j@q.bf");
                assertThat(d.getNomComplet()).isEqualTo("Awa Traoré");
            });
        }

        @Test
        @DisplayName("Sans nom ni prénom, le destinataire se nomme par son adresse")
        void nomParDefaut() {
            AppRole pilote = role("PILOTE");
            UserRepresentation anonyme = compte("u1", "S1", "u1@q.bf", true);
            anonyme.setFirstName(null);
            anonyme.setLastName(null);
            brancher(anonyme);
            when(affectations.findByRole_NameIgnoreCase("PILOTE")).thenReturn(List.of(affectation("u1", pilote)));

            assertThat(service.getUsersByRole("PILOTE", null))
                    .extracting(DestinataireDto::getNomComplet).containsExactly("u1@q.bf");
        }
    }

    @Nested
    @DisplayName("Création d'un compte")
    class Creation {

        private Response cree(String id) {
            Response reponse = mock(Response.class);
            when(reponse.getStatus()).thenReturn(201);
            when(reponse.getLocation()).thenReturn(URI.create("http://kc/admin/realms/quali/users/" + id));
            return reponse;
        }

        @Test
        @DisplayName("Le compte naît actif, avec structure, fonction, mot de passe temporaire et rôles connus")
        void creation() {
            Response reponseKc = cree("nouvel-id");
            when(comptes.create(any())).thenReturn(reponseKc);
            AppRole pilote = role("PILOTE");
            when(roles.findByName("PILOTE")).thenReturn(Optional.of(pilote));
            when(roles.findByName("INCONNU")).thenReturn(Optional.empty());
            when(jetons.emettre("nouvel-id", JetonDeCompte.Usage.VERIFICATION_COURRIEL,
                    JetonsDeCompte.DUREE_VERIFICATION)).thenReturn("jeton-v");
            KcUserDto demande = KcUserDto.builder().username("a.traore").email("a@q.bf").firstName("Awa")
                    .lastName("Traoré").structure("S1").fonction("Pilote")
                    .roles(List.of("PILOTE", "INCONNU")).build();

            KcUserDto rendu = service.createUser(demande);

            assertThat(rendu.getId()).isEqualTo("nouvel-id");
            ArgumentCaptor<UserRepresentation> envoye = ArgumentCaptor.forClass(UserRepresentation.class);
            verify(comptes).create(envoye.capture());
            UserRepresentation u = envoye.getValue();
            assertThat(u.isEnabled()).isTrue();
            assertThat(u.getAttributes()).containsEntry("structure", List.of("S1"))
                    .containsEntry("fonction", List.of("Pilote"));
            CredentialRepresentation motDePasse = u.getCredentials().get(0);
            assertThat(motDePasse.isTemporary()).isTrue();
            assertThat(motDePasse.getValue()).hasSize(8);

            ArgumentCaptor<UserRoleAssignment> attribue = ArgumentCaptor.forClass(UserRoleAssignment.class);
            verify(affectations).save(attribue.capture());
            assertThat(attribue.getAllValues()).singleElement().satisfies(a -> {
                assertThat(a.getUserId()).isEqualTo("nouvel-id");
                assertThat(a.getRole()).isSameAs(pilote);
            });
            verify(courriels).sendVerificationEmail(eq("a@q.bf"), eq("Awa"), eq("Traoré"),
                    eq(motDePasse.getValue()), eq(FRONT + "/verify-email?token=jeton-v&userId=nouvel-id"));
        }

        @Test
        @DisplayName("Les contrôles précèdent Keycloak : garde des rôles, puis plafond de la licence")
        void ordreDesControles() {
            Response reponseKc = cree("x");
            when(comptes.create(any())).thenReturn(reponseKc);

            service.createUser(KcUserDto.builder().username("a.traore").build());

            var ordre = inOrder(garde, plafond, comptes);
            ordre.verify(garde).exigerAttributionPermise(eq(null), any());
            ordre.verify(plafond).verifierAvantCreation();
            ordre.verify(comptes).create(any());
        }

        @Test
        @DisplayName("Plafond atteint : aucun compte n'est créé dans Keycloak")
        void plafondAtteint() {
            doThrow(new BusinessException("Plafond atteint", HttpStatus.FORBIDDEN))
                    .when(plafond).verifierAvantCreation();

            assertThatThrownBy(() -> service.createUser(KcUserDto.builder().username("a.traore").build()))
                    .isInstanceOf(BusinessException.class);
            verify(comptes, never()).create(any());
        }

        @Test
        @DisplayName("Un nom d'utilisateur vide : 400, et Keycloak n'est pas sollicité")
        void nomVide() {
            assertThatThrownBy(() -> service.createUser(KcUserDto.builder().username("  ").build()))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
            verifyNoInteractions(plafond);
            verify(comptes, never()).create(any());
        }

        @Test
        @DisplayName("Nom ou adresse déjà pris : 409, que le provisionnement reconnaît, et aucun rôle posé")
        void doublon() {
            Response conflit = mock(Response.class);
            when(conflit.getStatus()).thenReturn(409);
            when(comptes.create(any())).thenReturn(conflit);

            assertThatThrownBy(() -> service.createUser(KcUserDto.builder().username("admin")
                    .roles(List.of("PILOTE")).build()))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
            verify(affectations, never()).save(any());
            verifyNoInteractions(courriels, jetons);
        }

        @Test
        @DisplayName("Un courriel qui ne part pas ne défait pas le compte créé")
        void courrielEnEchec() {
            Response reponseKc = cree("x");
            when(comptes.create(any())).thenReturn(reponseKc);
            doThrow(new RuntimeException("SMTP injoignable")).when(courriels)
                    .sendVerificationEmail(any(), any(), any(), any(), any());

            assertThat(service.createUser(KcUserDto.builder().username("a.traore").build()).getId())
                    .isEqualTo("x");
        }

        @Test
        @DisplayName("Une adresse déjà vérifiée reçoit un mot de passe définitif")
        void adresseDejaVerifiee() {
            Response reponseKc = cree("x");
            when(comptes.create(any())).thenReturn(reponseKc);

            service.createUser(KcUserDto.builder().username("a.traore").emailVerified(true).build());

            ArgumentCaptor<UserRepresentation> envoye = ArgumentCaptor.forClass(UserRepresentation.class);
            verify(comptes).create(envoye.capture());
            assertThat(envoye.getValue().getCredentials().get(0).isTemporary()).isFalse();
        }
    }

    @Nested
    @DisplayName("Fiche et statut d'un compte")
    class FicheEtStatut {

        @Test
        @DisplayName("Modifier la fiche remplace les rôles applicatifs : les anciens sont retirés")
        void ficheRemplaceLesRoles() {
            UserRepresentation u = compte("cible", "S1", "c@q.bf", true);
            UserResource ressource = brancher(u);
            AppRole ancien = role("LECTEUR");
            AppRole nouveau = role("PILOTE");
            UserRoleAssignment existante = affectation("cible", ancien);
            when(affectations.findByUserId("cible")).thenReturn(List.of(existante));
            when(roles.findByName("PILOTE")).thenReturn(Optional.of(nouveau));

            service.updateUser(KcUserDto.builder().id("cible").firstName("Ali").lastName("Ouédraogo")
                    .email("c@q.bf").enabled(true).structure("S2").fonction("Auditeur")
                    .roles(List.of("PILOTE")).build());

            verify(ressource).update(u);
            assertThat(u.getFirstName()).isEqualTo("Ali");
            assertThat(u.getAttributes()).containsEntry("structure", List.of("S2"))
                    .containsEntry("fonction", List.of("Auditeur"));
            verify(affectations).delete(existante);
            verify(affectations).save(any(UserRoleAssignment.class));
        }

        @Test
        @DisplayName("Réactiver un compte reprend une place : le plafond est contrôlé ; désactiver, non")
        void statutEtPlafond() {
            UserRepresentation u = compte("cible", "S1", "c@q.bf", false);
            UserResource ressource = brancher(u);

            service.changeUserStatus("cible", true);
            verify(plafond).verifierAvantReactivation();
            verify(ressource).update(u);
            assertThat(u.isEnabled()).isTrue();

            service.changeUserStatus("cible", false);
            verify(plafond, org.mockito.Mockito.times(1)).verifierAvantReactivation();
            assertThat(u.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Plafond atteint à la réactivation : le compte reste désactivé")
        void reactivationAuPlafond() {
            UserRepresentation u = compte("cible", "S1", "c@q.bf", false);
            UserResource ressource = brancher(u);
            doThrow(new BusinessException("Plafond atteint", HttpStatus.FORBIDDEN))
                    .when(plafond).verifierAvantReactivation();

            assertThatThrownBy(() -> service.changeUserStatus("cible", true)).isInstanceOf(BusinessException.class);
            verify(ressource, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Vérification de l'adresse et réinitialisation")
    class VerificationEtReinitialisation {

        @Test
        @DisplayName("Vérification : le jeton est consommé avant que l'adresse ne soit marquée vérifiée")
        void verification() {
            UserRepresentation u = compte("cible", "S1", "c@q.bf", true);
            u.setRequiredActions(List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"));
            UserResource ressource = brancher(u);

            service.emailVerification("cible", "jeton");

            var ordre = inOrder(jetons, ressource);
            ordre.verify(jetons).consommer("cible", JetonDeCompte.Usage.VERIFICATION_COURRIEL, "jeton");
            ordre.verify(ressource).update(u);
            assertThat(u.isEmailVerified()).isTrue();
            assertThat(u.getRequiredActions()).isEmpty();
        }

        @Test
        @DisplayName("Vérification avec un jeton refusé : le compte n'est pas touché")
        void verificationJetonRefuse() {
            doThrow(new BusinessException("Lien invalide", HttpStatus.BAD_REQUEST))
                    .when(jetons).consommer(anyString(), any(), anyString());

            assertThatThrownBy(() -> service.emailVerification("cible", "faux")).isInstanceOf(BusinessException.class);
            verify(comptes, never()).get(anyString());
        }

        @Test
        @DisplayName("Une adresse vide ne déclenche rien")
        void adresseVide() {
            service.initiatePasswordReset("  ");
            service.initiatePasswordReset(null);

            verifyNoInteractions(jetons, courriels);
        }

        @Test
        @DisplayName("Adresse inconnue, partagée ou de compte désactivé : silence, ni jeton ni courriel")
        void sansCompteActifUnique() {
            when(comptes.searchByEmail("inconnu@q.bf", true)).thenReturn(List.of());
            when(comptes.searchByEmail("double@q.bf", true)).thenReturn(List.of(
                    compte("a", null, "double@q.bf", true), compte("b", null, "double@q.bf", true)));
            when(comptes.searchByEmail("inactif@q.bf", true))
                    .thenReturn(List.of(compte("c", null, "inactif@q.bf", false)));

            assertThatCode(() -> {
                service.initiatePasswordReset("inconnu@q.bf");
                service.initiatePasswordReset("double@q.bf");
                service.initiatePasswordReset("inactif@q.bf");
            }).doesNotThrowAnyException();
            verifyNoInteractions(jetons, courriels);
        }

        @Test
        @DisplayName("Compte actif unique : un jeton de réinitialisation, envoyé à l'adresse du compte")
        void compteActif() {
            when(comptes.searchByEmail("a@q.bf", true)).thenReturn(List.of(compte("u1", null, "A@q.bf", true)));
            when(jetons.emettre("u1", JetonDeCompte.Usage.REINITIALISATION, JetonsDeCompte.DUREE_REINITIALISATION))
                    .thenReturn("jeton-r");

            service.initiatePasswordReset(" a@q.bf ");

            verify(courriels).sendReinitializePasswordEmail("A@q.bf",
                    FRONT + "/reset-password?token=jeton-r&userId=u1");
        }

        @Test
        @DisplayName("Un envoi en échec ne se dit pas : la réponse reste la même pour toute adresse")
        void envoiEnEchec() {
            when(comptes.searchByEmail("a@q.bf", true)).thenReturn(List.of(compte("u1", null, "a@q.bf", true)));
            doThrow(new RuntimeException("SMTP")).when(courriels).sendReinitializePasswordEmail(any(), any());

            assertThatCode(() -> service.initiatePasswordReset("a@q.bf")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Connexion et session")
    class Connexion {

        private KcTokenDto jetonsEmis() {
            return KcTokenDto.builder().accessToken("acces-secret").refreshToken("renouv-secret")
                    .expiresIn(300).refreshExpiresIn(1800).tokenType("Bearer").build();
        }

        @Test
        @DisplayName("Les jetons partent en cookies HttpOnly, jamais dans le corps de la réponse")
        void jetonsEnCookies() {
            when(jetonsKeycloak.getAccessToken(any())).thenReturn(jetonsEmis());
            when(comptes.search("u1", true)).thenReturn(List.of(compte("u1", "S1", "a@q.bf", true)));
            MockHttpServletResponse reponse = new MockHttpServletResponse();

            Map<String, Object> corps = service.login(
                    KcLoginRequestDto.builder().username("u1").password("p").build(), reponse);

            assertThat(corps.toString()).doesNotContain("acces-secret", "renouv-secret");
            assertThat(reponse.getHeaders("Set-Cookie"))
                    .anySatisfy(c -> assertThat(c).startsWith("access_token=acces-secret").contains("HttpOnly"))
                    .anySatisfy(c -> assertThat(c).startsWith("refresh_token=renouv-secret").contains("HttpOnly"));
        }

        @Test
        @DisplayName("La connexion rend structure, fonction, rôles, permissions et licence de la direction")
        void contenuDeLaSession() {
            when(jetonsKeycloak.getAccessToken(any())).thenReturn(jetonsEmis());
            when(comptes.search("u1", true)).thenReturn(List.of(compte("u1", "S1", "a@q.bf", true)));
            AppRole pilote = role("PILOTE", "NC_READ", "NC_WRITE");
            AppRole autre = role("AUDITEUR", "AUDIT_READ");
            when(affectations.findByUserId("u1")).thenReturn(List.of(affectation("u1", pilote)));
            when(roles.findAll()).thenReturn(List.of(pilote, autre));
            StructureDto direction = new StructureDto();
            direction.setLicenceActive(true);
            direction.setLicenseDaysRemaining(42L);
            direction.setModulesSubscribed(List.of("NON_CONFORMITE"));
            when(structures.getDirection()).thenReturn(direction);

            Map<String, Object> corps = service.login(
                    KcLoginRequestDto.builder().username("u1").password("p").build(), new MockHttpServletResponse());

            assertThat(corps).containsEntry("licenseActive", true).containsEntry("licenseDaysRemaining", 42)
                    .containsEntry("modulesSubscribed", List.of("NON_CONFORMITE"))
                    .containsEntry("appRoles", List.of("PILOTE"))
                    .containsEntry("fonction", "Pilote");
            assertThat((List<String>) corps.get("permissions")).containsExactlyInAnyOrder("NC_READ", "NC_WRITE");
            assertThat((Map<String, Object>) corps.get("user")).containsEntry("structure", "S1");
        }

        @Test
        @DisplayName("SUPERADMIN, ancienne graphie, vaut SUPER_ADMIN et reçoit toutes les permissions")
        void superAdmin() {
            when(jetonsKeycloak.getAccessToken(any())).thenReturn(jetonsEmis());
            when(comptes.search("u1", true)).thenReturn(List.of(compte("u1", "S1", "a@q.bf", true)));
            AppRole ancien = role("SuperAdmin");
            when(affectations.findByUserId("u1")).thenReturn(List.of(affectation("u1", ancien)));
            when(roles.findAll()).thenReturn(List.of(ancien, role("PILOTE", "NC_READ"), role("AUDIT", "AUDIT_READ", "NC_READ")));

            Map<String, Object> corps = service.login(
                    KcLoginRequestDto.builder().username("u1").password("p").build(), new MockHttpServletResponse());

            assertThat((List<String>) corps.get("appRoles")).containsExactly("SuperAdmin", "SUPER_ADMIN");
            assertThat((List<String>) corps.get("permissions")).containsExactlyInAnyOrder("NC_READ", "AUDIT_READ");
        }

        @Test
        @DisplayName("Référentiel injoignable : la connexion aboutit, licence tenue pour inactive")
        void referentielInjoignable() {
            when(jetonsKeycloak.getAccessToken(any())).thenReturn(jetonsEmis());
            when(comptes.search("u1", true)).thenReturn(List.of(compte("u1", "S1", "a@q.bf", true)));
            when(structures.getDirection()).thenThrow(new RuntimeException("503"));

            Map<String, Object> corps = service.login(
                    KcLoginRequestDto.builder().username("u1").password("p").build(), new MockHttpServletResponse());

            assertThat(corps).containsEntry("licenseActive", false).containsEntry("licenseDaysRemaining", 0)
                    .containsEntry("modulesSubscribed", List.of());
        }

        @Test
        @DisplayName("Renouvellement accepté : les deux cookies sont reposés")
        void renouvellement() {
            MockHttpServletRequest requete = new MockHttpServletRequest();
            requete.setCookies(new Cookie("refresh_token", "ancien"));
            when(jetonsKeycloak.getRefreshToken("ancien")).thenReturn(jetonsEmis());
            MockHttpServletResponse reponse = new MockHttpServletResponse();

            assertThat(service.refreshToken(requete, reponse).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reponse.getHeaders("Set-Cookie")).hasSize(2)
                    .anySatisfy(c -> assertThat(c).startsWith("refresh_token=renouv-secret"));
        }

        @Test
        @DisplayName("Déconnexion : les cookies sont effacés, même si Keycloak refuse la révocation")
        void deconnexion() {
            MockHttpServletRequest requete = new MockHttpServletRequest();
            requete.setCookies(new Cookie("refresh_token", "rt"));
            doThrow(new RuntimeException("Keycloak injoignable")).when(jetonsKeycloak).revokeToken("rt");
            MockHttpServletResponse reponse = new MockHttpServletResponse();

            assertThat(service.logout(requete, reponse).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reponse.getHeaders("Set-Cookie"))
                    .anySatisfy(c -> assertThat(c).startsWith("access_token=;").contains("Max-Age=0"))
                    .anySatisfy(c -> assertThat(c).startsWith("refresh_token=;").contains("Max-Age=0"));
        }

        @Test
        @DisplayName("Déconnexion sans cookie : rien à révoquer, les cookies sont tout de même effacés")
        void deconnexionSansCookie() {
            MockHttpServletResponse reponse = new MockHttpServletResponse();

            service.logout(new MockHttpServletRequest(), reponse);

            verify(jetonsKeycloak, never()).revokeToken(anyString());
            assertThat(reponse.getHeaders("Set-Cookie")).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Annuaire, rôles et permissions de l'utilisateur connecté")
    class Annuaire {

        @Test
        @DisplayName("L'annuaire se pagine ; une page au-delà de la fin est vide, le total conservé")
        void pagination() {
            when(comptes.list()).thenReturn(List.of(compte("a", "S1", "a@q.bf", true),
                    compte("b", "S1", "b@q.bf", true), compte("c", "S2", "c@q.bf", true)));

            Page<KcUserDto> premiere = service.getAllUsers(PageRequest.of(0, 2));
            Page<KcUserDto> horsBorne = service.getAllUsers(PageRequest.of(5, 2));

            assertThat(premiere.getContent()).extracting(KcUserDto::getId).containsExactly("a", "b");
            assertThat(premiere.getTotalElements()).isEqualTo(3);
            assertThat(premiere.getContent().get(0).getStructure()).isEqualTo("S1");
            assertThat(horsBorne.getContent()).isEmpty();
            assertThat(horsBorne.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("Les membres d'une structure : les autres structures et les comptes sans structure sont exclus")
        void parStructure() {
            when(comptes.list()).thenReturn(List.of(compte("a", "S1", "a@q.bf", true),
                    compte("b", "S2", "b@q.bf", true), compte("c", null, "c@q.bf", true)));

            Page<KcUserDto> membres = service.getUsersByStructure("S1", PageRequest.of(0, 10));

            assertThat(membres.getContent()).extracting(KcUserDto::getId).containsExactly("a");
            assertThat(membres.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Mes permissions : union sans doublon de celles de mes rôles, et d'eux seuls")
        void mesPermissions() {
            connecter(MOI);
            AppRole pilote = role("PILOTE", "NC_READ", "NC_WRITE");
            AppRole lecteur = role("LECTEUR", "NC_READ");
            when(affectations.findByUserId(MOI)).thenReturn(List.of(affectation(MOI, pilote), affectation(MOI, lecteur)));
            when(roles.findAll()).thenReturn(List.of(pilote, lecteur, role("ADMIN", "MANAGE_USER")));

            assertThat(service.getMyPermissions()).containsExactlyInAnyOrder("NC_READ", "NC_WRITE");
        }

        @Test
        @DisplayName("Mes rôles : identifiant et nom, sans doublon ; sans jeton, refusé")
        void mesRoles() {
            connecter(MOI);
            AppRole pilote = role("PILOTE");
            when(affectations.findByUserId(MOI)).thenReturn(List.of(
                    affectation(MOI, pilote), affectation(MOI, pilote), affectation(MOI, null)));

            assertThat(service.getMyRoles()).containsExactly(
                    Map.of("id", pilote.getId().toString(), "name", "PILOTE"));

            SecurityContextHolder.clearContext();
            assertThatThrownBy(service::getMyRoles).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(service::getMyPermissions).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("La fiche d'un compte rend aussi le téléphone, que l'écran « mon profil » affiche")
        void ficheAvecTelephone() {
            UserRepresentation u = compte("u1", "S1", "a@q.bf", true);
            u.getAttributes().put("phoneNumber", List.of("+22670000000"));
            brancher(u);

            assertThat(service.getUserById("u1")).containsEntry("phoneNumber", "+22670000000")
                    .containsEntry("structure", "S1").containsEntry("enabled", true);
        }
    }
}
