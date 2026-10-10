package com.qualiapproche.userservice.service;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.UserRoleAssignment;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * « Nul ne donne ce qu'il n'a pas » : les règles qui bornent un gestionnaire des utilisateurs ou
 * des rôles à ses propres habilitations.
 *
 * <p>Chaque cas refusé ici était, avant la garde, une prise de contrôle en un ou deux gestes :
 * s'attribuer {@code SUPER_ADMIN}, attribuer à un complice un rôle plus fort que le sien, retirer
 * à un administrateur ses rôles, ou ajouter à son rôle une permission qu'on n'a pas.</p>
 */
class GardeDesHabilitationsTest {

    private static final String MOI = "moi";
    private static final String AUTRE = "autre";

    private final UserRoleAssignmentRepository affectations = mock(UserRoleAssignmentRepository.class);
    private final GardeDesHabilitations garde = new GardeDesHabilitations(affectations, new PermissionChecker());

    private final AppRole gestionnaire = role("GESTIONNAIRE", "MANAGE_USER", "nc-read", "nc-write");
    private final AppRole lecteur = role("LECTEUR", "nc-read");
    private final AppRole licence = role("LICENCE", "licence-write");
    private final AppRole superAdmin = role("SUPER_ADMIN", "MANAGE_USER");

    @BeforeEach
    void connecter() {
        connecterSous("a.traore");
        porte(MOI, gestionnaire);
        porte(AUTRE);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecterSous(String nom) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "RS256").subject(MOI)
                .claim("preferred_username", nom)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private static AppRole role(String nom, String... permissions) {
        AppRole role = new AppRole();
        role.setId(UUID.randomUUID());
        role.setName(nom);
        role.setPermissions(new ArrayList<>(Arrays.asList(permissions)));
        return role;
    }

    private void porte(String userId, AppRole... roles) {
        when(affectations.findByUserId(userId)).thenReturn(Arrays.stream(roles)
                .<UserRoleAssignment>map(r -> UserRoleAssignment.builder().userId(userId).role(r).build()).toList());
    }

    private static void refuse(Runnable geste, String extrait) {
        assertThatThrownBy(geste::run).isInstanceOfSatisfying(BusinessException.class, e -> {
            org.assertj.core.api.Assertions.assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            org.assertj.core.api.Assertions.assertThat(e.getMessage()).contains(extrait);
        });
    }

    @Test
    @DisplayName("Attribuer un rôle que je couvre : permis")
    void attribution_couverte() {
        assertThatCode(() -> garde.exigerAttributionPermise(AUTRE, List.of(lecteur))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Attribuer un rôle porteur d'une permission que je n'ai pas : refusé, la permission nommée")
    void attribution_nonCouverte() {
        refuse(() -> garde.exigerAttributionPermise(AUTRE, List.of(licence)), "licence-write");
    }

    @Test
    @DisplayName("Attribuer SUPER_ADMIN sans l'être : refusé, quelles que soient ses permissions")
    void attribution_superAdmin() {
        refuse(() -> garde.exigerAttributionPermise(AUTRE, List.of(superAdmin)), "SUPER_ADMIN");
    }

    @Test
    @DisplayName("Retirer un rôle que je ne pourrais pas attribuer : refusé aussi")
    void retrait_nonCouvert() {
        porte(AUTRE, licence);

        refuse(() -> garde.exigerAttributionPermise(AUTRE, List.of()), "licence-write");
    }

    @Test
    @DisplayName("Modifier mes propres rôles : refusé, même pour un rôle que je couvre")
    void sesPropresRoles() {
        refuse(() -> garde.exigerAttributionPermise(MOI, List.of(gestionnaire, lecteur)), "propre compte");
    }

    @Test
    @DisplayName("Enregistrer ma fiche sans toucher à mes rôles : permis")
    void sesPropresRoles_inchanges() {
        assertThatCode(() -> garde.exigerAttributionPermise(MOI, List.of(gestionnaire))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un SUPER_ADMIN attribue tout à autrui, mais pas à lui-même")
    void superAdmin_sansBorne() {
        porte(MOI, superAdmin);

        assertThatCode(() -> garde.exigerAttributionPermise(AUTRE, List.of(licence, superAdmin))).doesNotThrowAnyException();
        refuse(() -> garde.exigerAttributionPermise(MOI, List.of(superAdmin, licence)), "propre compte");
    }

    @Test
    @DisplayName("Un appel de service attribue sans borne : c'est ainsi que naît le premier administrateur")
    void appelDeService() {
        connecterSous("service-account-quali-sira");
        porte(MOI);

        assertThatCode(() -> garde.exigerAttributionPermise(null, List.of(superAdmin))).doesNotThrowAnyException();
        assertThatCode(() -> garde.exigerAscendantSur(AUTRE, "x")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un compte en création ne porte rien : seuls les rôles demandés sont contrôlés")
    void creation() {
        assertThatCode(() -> garde.exigerAttributionPermise(null, List.of(lecteur))).doesNotThrowAnyException();
        refuse(() -> garde.exigerAttributionPermise(null, List.of(licence)), "licence-write");
    }

    @Test
    @DisplayName("Réinitialiser le mot de passe d'un compte plus privilégié : refusé")
    void ascendant_refuse() {
        porte(AUTRE, licence);
        refuse(() -> garde.exigerAscendantSur(AUTRE, "réinitialiser son mot de passe"), "réinitialiser son mot de passe");

        porte(AUTRE, superAdmin);
        refuse(() -> garde.exigerAscendantSur(AUTRE, "désactiver ce compte"), "désactiver ce compte");
    }

    @Test
    @DisplayName("Sur un compte que je couvre, ou sur le mien : permis")
    void ascendant_permis() {
        porte(AUTRE, lecteur);

        assertThatCode(() -> garde.exigerAscendantSur(AUTRE, "x")).doesNotThrowAnyException();
        assertThatCode(() -> garde.exigerAscendantSur(MOI, "x")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Ajouter à un rôle, ou en retirer, une permission que je n'ai pas : refusé")
    void contenuDUnRole() {
        refuse(() -> garde.exigerPermissionsDetenues(List.of("nc-read"), List.of("nc-read", "licence-write")),
                "licence-write");
        refuse(() -> garde.exigerPermissionsDetenues(List.of("licence-write"), List.of()), "licence-write");
        assertThatCode(() -> garde.exigerPermissionsDetenues(List.of("licence-write"), List.of("licence-write", "nc-read")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sans jeton, rien ne se décide")
    void sansJeton() {
        SecurityContextHolder.clearContext();

        refuse(() -> garde.exigerAscendantSur(AUTRE, "x"), "reconnectez-vous");
    }

    @Test
    @DisplayName("Écrire un rôle Keycloak : réservé au SUPER_ADMIN lu en base, même pour un compte de service")
    void superAdminExige() {
        refuse(() -> garde.exigerSuperAdmin("créer un rôle Keycloak"), "SUPER_ADMIN");

        connecterSous("service-account-referentiel");
        refuse(() -> garde.exigerSuperAdmin("créer un rôle Keycloak"), "SUPER_ADMIN");

        connecterSous("a.traore");
        porte(MOI, superAdmin);
        assertThatCode(() -> garde.exigerSuperAdmin("créer un rôle Keycloak")).doesNotThrowAnyException();
    }
}
