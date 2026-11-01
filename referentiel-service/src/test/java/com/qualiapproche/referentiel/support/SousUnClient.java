package com.qualiapproche.referentiel.support;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

/**
 * Exécute chaque test sous le jeton d'un utilisateur du client {@link #CLIENT} : la licence, le
 * code partenaire, la direction se lisent désormais pour un client, et non pour l'installation.
 */
public class SousUnClient implements BeforeEachCallback, AfterEachCallback {

    public static final UUID CLIENT = UUID.fromString("6f1c0f2e-6a1b-4f43-9a55-0c1f7f0b1a11");

    @Override
    public void beforeEach(ExtensionContext contexte) {
        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject("utilisateur-du-client")
                .claim("direction_id", CLIENT.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Override
    public void afterEach(ExtensionContext contexte) {
        SecurityContextHolder.clearContext();
    }
}
