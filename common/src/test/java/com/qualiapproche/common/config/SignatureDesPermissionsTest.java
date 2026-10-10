package com.qualiapproche.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un service ne croit l'en-tête X-User-Permissions que signé par la passerelle, pour le jeton de
 * la requête. Il le croyait tel quel : un appel direct avec un jeton valide et
 * « X-User-Permissions: SUPER_ADMIN » ouvrait tout.
 */
class SignatureDesPermissionsTest {

    private static final String SECRET = "secret-de-test";
    private static final String JETON = "jeton-abc";
    private static final String PERMISSIONS = "AUDIT-READ,MANAGE_USER";
    /** Le même vecteur est fixé côté passerelle (api-gateway, SignatureDesPermissionsTest). */
    private static final String VECTEUR = "JGDYHwZnUP-lRmgZ5UmDUyMIY7aGBvIJxc88fqELsXw";

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Le calcul est celui de la passerelle : même vecteur des deux côtés")
    void meme_vecteur_que_la_passerelle() {
        assertThat(SignatureDesPermissions.signer(SECRET, JETON, PERMISSIONS)).isEqualTo(VECTEUR);
    }

    @Test
    @DisplayName("Autre jeton, autres permissions, autre secret ou rien : la signature ne vaut pas")
    void la_signature_ne_vaut_que_pour_son_jeton() {
        assertThat(SignatureDesPermissions.verifier(SECRET, JETON, PERMISSIONS, VECTEUR)).isTrue();
        assertThat(SignatureDesPermissions.verifier(SECRET, "autre-jeton", PERMISSIONS, VECTEUR)).isFalse();
        assertThat(SignatureDesPermissions.verifier(SECRET, JETON, PERMISSIONS + ",SUPER_ADMIN", VECTEUR)).isFalse();
        assertThat(SignatureDesPermissions.verifier("autre-secret", JETON, PERMISSIONS, VECTEUR)).isFalse();
        assertThat(SignatureDesPermissions.verifier(null, JETON, PERMISSIONS, VECTEUR)).isFalse();
        assertThat(SignatureDesPermissions.verifier(SECRET, JETON, PERMISSIONS, null)).isFalse();
    }

    private void requete(String permissions, String signature) {
        Jwt jwt = Jwt.withTokenValue(JETON).header("alg", "RS256").subject("u")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader("X-User-Permissions", permissions);
        if (signature != null) {
            http.addHeader(SignatureDesPermissions.EN_TETE, signature);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(http));
    }

    @Test
    @DisplayName("Un en-tête signé par la passerelle est cru")
    void entete_signe_cru() {
        requete(PERMISSIONS, VECTEUR);
        assertThat(new PermissionChecker(SECRET).detient("MANAGE_USER")).isTrue();
    }

    @Test
    @DisplayName("Un en-tête forgé — sans signature, ou signature d'autres permissions — est ignoré")
    void entete_forge_ignore() {
        requete("SUPER_ADMIN", null);
        assertThat(new PermissionChecker(SECRET).detient("SUPER_ADMIN")).isFalse();

        requete("SUPER_ADMIN", VECTEUR);
        assertThat(new PermissionChecker(SECRET).detient("SUPER_ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Sans secret configuré, l'en-tête n'est jamais cru, même signé")
    void sans_secret_rien() {
        requete(PERMISSIONS, VECTEUR);
        assertThat(new PermissionChecker().detient("MANAGE_USER")).isFalse();
    }
}
