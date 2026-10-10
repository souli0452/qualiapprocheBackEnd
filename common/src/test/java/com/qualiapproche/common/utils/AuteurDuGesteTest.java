package com.qualiapproche.common.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un geste fait hors de sa requête — un rappel du circuit — est attribué à la personne qui a
 * décidé, sans que cela lui donne le moindre droit.
 */
class AuteurDuGesteTest {

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sans jeton ni auteur transmis, le geste reste sans auteur, comme avant")
    void sansAuteur() {
        assertThat(SecurityUtils.getAuteurId()).isNull();
        assertThat(SecurityUtils.getAuteurNom()).isEqualTo("Système");
    }

    @Test
    @DisplayName("L'auteur transmis est attribué pendant le geste, et seulement pendant")
    void auteurTransmis() {
        AuteurDuGeste.pour("u-42", "Awa Ouédraogo", () -> {
            assertThat(SecurityUtils.getAuteurId()).isEqualTo("u-42");
            assertThat(SecurityUtils.getAuteurNom()).isEqualTo("Awa Ouédraogo");
            // L'attribution ne vaut pas identité : rien de ce qui décide d'un droit ne le voit.
            assertThat(SecurityUtils.getCurrentUserId()).isNull();
        });
        assertThat(SecurityUtils.getAuteurId()).isNull();
    }

    @Test
    @DisplayName("Un geste imbriqué rend la main à l'auteur du geste englobant")
    void imbrique() {
        AuteurDuGeste.pour("u-1", "Un", () -> {
            AuteurDuGeste.pour("u-2", "Deux", () -> {
                assertThat(SecurityUtils.getAuteurId()).isEqualTo("u-2");
            });
            assertThat(SecurityUtils.getAuteurId()).isEqualTo("u-1");
        });
    }

    @Test
    @DisplayName("Dans une requête, l'utilisateur du jeton reste l'auteur")
    void jetonPrioritaire() {
        Jwt jeton = Jwt.withTokenValue("t").header("alg", "none").subject("u-jeton").claim("name", "Jeton").build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jeton, null));

        AuteurDuGeste.pour("u-42", "Awa", () -> {
            assertThat(SecurityUtils.getAuteurId()).isEqualTo("u-jeton");
            assertThat(SecurityUtils.getAuteurNom()).isEqualTo("Jeton");
        });
    }
}
