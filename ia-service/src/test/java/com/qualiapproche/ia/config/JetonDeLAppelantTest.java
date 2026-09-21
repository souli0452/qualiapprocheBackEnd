package com.qualiapproche.ia.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que l'assistant emporte quand il interroge les services métier — et ce qu'il n'emporte jamais.
 *
 * <p>C'est la garantie de cloisonnement du module, et la seule. Les questions prédéfinies puisent
 * dans les non-conformités, les plans d'action et les documents de la personne qui les pose :
 * l'appel sortant doit porter <b>son</b> jeton, et rien d'autre. Les quatre autres modules se
 * replient sur un jeton de service quand l'utilisateur manque ; ici ce repli serait exactement le
 * trou que cette classe ferme — l'assistant rendrait alors les dossiers de tous à chacun.</p>
 *
 * <p>Aucun test ne couvrait cela jusqu'ici : une refonte de la configuration pouvait rétablir le
 * repli sans que rien ne le signale.</p>
 */
class JetonDeLAppelantTest {

    private final RequestInterceptor intercepteur = new FeignConfig().requestInterceptor();

    @AfterEach
    void ranger() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void connecter(String valeurDuJeton) {
        Jwt jwt = Jwt.withTokenValue(valeurDuJeton)
                .header("alg", "none")
                .claim("sub", "agent-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private RequestTemplate appel() {
        RequestTemplate gabarit = new RequestTemplate();
        gabarit.uri("/api/v1/non-conformite/a-traiter");
        return gabarit;
    }

    @Test
    @DisplayName("le jeton de l'appelant part avec l'appel")
    void leJetonDeLAppelant_part() {
        connecter("jeton-de-la-personne");
        RequestTemplate gabarit = appel();

        intercepteur.apply(gabarit);

        assertThat(gabarit.headers().get("Authorization"))
                .containsExactly("Bearer jeton-de-la-personne");
    }

    @Test
    @DisplayName("sans utilisateur authentifié, RIEN n'est posé : aucun repli sur un jeton de service")
    void sansUtilisateur_aucunRepli() {
        RequestTemplate gabarit = appel();

        intercepteur.apply(gabarit);

        // L'appel partira nu et sera refusé par le service visé. C'est le comportement voulu :
        // mieux vaut un refus franc qu'une réponse obtenue avec des droits qui ne sont ceux de
        // personne.
        assertThat(gabarit.headers()).doesNotContainKey("Authorization");
        assertThat(gabarit.headers()).doesNotContainKey("X-User-Permissions");
    }

    @Test
    @DisplayName("une authentification qui n'est pas un jeton JWT ne donne rien non plus")
    void authentificationNonJwt_neDonneRien() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agent-1", "secret", List.of()));
        RequestTemplate gabarit = appel();

        intercepteur.apply(gabarit);

        assertThat(gabarit.headers()).doesNotContainKey("Authorization");
    }

    @Test
    @DisplayName("les permissions de l'appelant suivent son jeton")
    void lesPermissions_suiventLeJeton() {
        connecter("jeton");
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addHeader("X-User-Permissions", "nc-read,document-read");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requete));
        RequestTemplate gabarit = appel();

        intercepteur.apply(gabarit);

        assertThat(gabarit.headers().get("X-User-Permissions"))
                .containsExactly("nc-read,document-read");
    }

    @Test
    @DisplayName("un en-tête de permissions vide n'est pas propagé : il vaudrait affirmation de rien")
    void permissionsVides_nePassentPas() {
        connecter("jeton");
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addHeader("X-User-Permissions", "   ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requete));
        RequestTemplate gabarit = appel();

        intercepteur.apply(gabarit);

        assertThat(gabarit.headers()).doesNotContainKey("X-User-Permissions");
    }
}
