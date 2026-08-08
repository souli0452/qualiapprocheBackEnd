package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.config.PermissionChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Qui peut poser une licence.
 *
 * <p>Les deux points d'entrée d'écriture étaient gardés par
 * {@code hasAnyAuthority('SUPER_ADMIN', …)}, seule habilitation du dépôt à ne pas passer par
 * {@link PermissionChecker}. Elle ne pouvait aboutir pour personne : les rôles du jeton arrivent
 * préfixés {@code ROLE_} (cf. {@code KeycloakRoleConverter}), et les permissions applicatives ne
 * sont pas des autorités — la passerelle les transmet par l'en-tête {@code X-User-Permissions},
 * que {@code hasAnyAuthority} ne lit pas. Démarrer l'essai gratuit répondait donc 403, y compris
 * au super administrateur, sur une installation où il est le seul compte existant.</p>
 */
class HabilitationDeLaLicenceTest {

    private final PermissionChecker perm = new PermissionChecker();
    private final LicenceController controleur = new LicenceController(null);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void connecteAvec(String... autorites) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("essai", "n/a",
                        List.of(autorites).stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    @DisplayName("Le super administrateur peut poser une licence, rôle préfixé compris")
    void superAdmin_peutPoserUneLicence() {
        connecteAvec("ROLE_SUPER_ADMIN");

        assertThat(perm.canCreate(controleur)).isTrue();
    }

    @Test
    @DisplayName("La permission applicative de configuration suffit aussi")
    void configGlobalWrite_suffit() {
        // Telle que la passerelle la transmet : en majuscules, sans préfixe.
        connecteAvec("CONFIG-GLOBAL-WRITE");

        assertThat(perm.canCreate(controleur)).isTrue();
    }

    @Test
    @DisplayName("Un utilisateur ordinaire ne pose pas de licence")
    void agent_nePosePasDeLicence() {
        connecteAvec("ROLE_AGENT", "NC-WRITE");

        assertThat(perm.canCreate(controleur)).isFalse();
    }

    @Test
    @DisplayName("Sans authentification, rien n'est ouvert")
    void anonyme_nePosePasDeLicence() {
        assertThat(perm.canCreate(controleur)).isFalse();
    }
}
