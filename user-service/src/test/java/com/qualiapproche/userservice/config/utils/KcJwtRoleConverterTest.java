package com.qualiapproche.userservice.config.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autorités tirées du jeton : les rôles du realm, et eux seuls, deviennent des {@code ROLE_*}.
 *
 * <p>Ce sont ces autorités que {@code PermissionChecker} compare aux exigences : un jeton sans
 * {@code realm_access} ne doit rien ouvrir, et un rôle de client ne doit pas passer pour un rôle du
 * realm.</p>
 */
class KcJwtRoleConverterTest {

    private final KcJwtRoleConverter convertisseur = new KcJwtRoleConverter("qualisira-front");

    private static Jwt jeton(Map<String, Object> claims) {
        Jwt.Builder b = Jwt.withTokenValue("jeton").header("alg", "RS256").subject("u1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(b::claim);
        return b.build();
    }

    @Test
    @DisplayName("Chaque rôle du realm devient une autorité préfixée ROLE_")
    void rolesDuRealm() {
        Jwt jwt = jeton(Map.of("realm_access", Map.of("roles", List.of("PILOTE", "offline_access"))));

        assertThat(convertisseur.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_PILOTE", "ROLE_offline_access");
    }

    @Test
    @DisplayName("Sans realm_access, ou sans rôle dedans : aucune autorité")
    void sansRoles() {
        assertThat(convertisseur.convert(jeton(Map.of("scope", "openid")))).isEmpty();
        assertThat(convertisseur.convert(jeton(Map.of("realm_access", Map.of("autre", "x"))))).isEmpty();
        assertThat(convertisseur.convert(jeton(Map.of("realm_access", Map.of("roles", List.of()))))).isEmpty();
    }

    @Test
    @DisplayName("Les rôles d'un client (resource_access) ne deviennent pas des autorités")
    void rolesDeClientIgnores() {
        Jwt jwt = jeton(Map.of("resource_access",
                Map.of("qualisira-front", Map.of("roles", List.of("SUPER_ADMIN")))));

        assertThat(convertisseur.convert(jwt)).isEmpty();
    }
}
