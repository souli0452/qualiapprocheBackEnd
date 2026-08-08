package com.qualiapproche.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reconnaissance d'un appel de service, sur laquelle reposent les points d'entrée techniques.
 *
 * <p>Callbacks d'avancement, déclaration d'un fait, redésignation d'un titulaire : ces points
 * écrivent l'état métier sans décision de circuit. La frontière est le {@code preferred_username}
 * posé par Keycloak sur les jetons {@code client_credentials} — un utilisateur ne peut pas se
 * l'attribuer, il faudrait le secret du client. Si cette reconnaissance se trompe dans un sens, un
 * agent peut forcer un statut ; dans l'autre, plus aucune notification n'aboutit.</p>
 */
class AppelDeServiceTest {

    private final PermissionChecker checker = new PermissionChecker();

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jeton(String preferredUsername) {
        Jwt.Builder jwt = Jwt.withTokenValue("jeton")
                .header("alg", "RS256")
                .subject("11111111-1111-4111-8111-111111111111")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (preferredUsername != null) {
            jwt.claim("preferred_username", preferredUsername);
        }
        return jwt.build();
    }

    private void connecter(String preferredUsername, String... roles) {
        List<SimpleGrantedAuthority> autorites = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jeton(preferredUsername), autorites));
    }

    @Test
    @DisplayName("Le compte de service Keycloak est reconnu")
    void compteDeService_reconnu() {
        connecter("service-account-quali-sira");

        assertThat(checker.appelDeService()).isTrue();
    }

    @Test
    @DisplayName("Un utilisateur ordinaire n'est pas un service, quels que soient ses rôles")
    void utilisateur_refuse() {
        connecter("awa.traore", "ROLE_SUPER_ADMIN", "ROLE_RQ");

        // Même l'administrateur passe par les décisions de circuit : lui ouvrir les callbacks
        // permettrait de forcer un statut sans trace de décision.
        assertThat(checker.appelDeService()).isFalse();
    }

    @Test
    @DisplayName("Un nom d'utilisateur imitant le préfixe ne suffit que s'il vient de Keycloak")
    void prefixeDansLeJeton_faitFoi() {
        // Le préfixe est jugé sur le claim du jeton signé, pas sur une valeur client : un
        // utilisateur nommé « service-account-x » dans Keycloak serait un compte créé par
        // l'administrateur du realm, ce qui est précisément une décision d'administration.
        connecter("service-account-import");

        assertThat(checker.appelDeService()).isTrue();
    }

    @Test
    @DisplayName("Le rôle SERVICE_TECHNIQUE est admis en alternative")
    void roleTechnique_admis() {
        connecter("integration-batch", "ROLE_SERVICE_TECHNIQUE");

        assertThat(checker.appelDeService()).isTrue();
    }

    @Test
    @DisplayName("Sans jeton JWT, refus — jamais d'accès par défaut")
    void sansJeton_refuse() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("x", "y"));

        assertThat(checker.appelDeService()).isFalse();
    }

    @Test
    @DisplayName("Jeton sans preferred_username : refus, pas d'exception")
    void jetonSansNom_refuse() {
        connecter(null);

        assertThat(checker.appelDeService()).isFalse();
    }

    @Test
    @DisplayName("preferred_username ne contenant le préfixe qu'au milieu : refusé")
    void prefixeAuMilieu_refuse() {
        connecter("mon-service-account-perso");

        assertThat(checker.appelDeService()).isFalse();
    }
}
