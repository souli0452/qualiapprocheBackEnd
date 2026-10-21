package com.qualiapproche.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les en-têtes de sécurité posés sur chaque réponse de la passerelle.
 *
 * <p>Un scanner les réclame sur toute réponse ; mais un en-tête posé par le service aval doit être
 * respecté, sans quoi la passerelle déciderait à sa place de ce que vaut une réponse qu'elle ne
 * connaît pas.</p>
 */
class FiltreEnTetesDeSecuriteTest {

    private final FiltreEnTetesDeSecurite filtre = new FiltreEnTetesDeSecurite();

    /** Fait passer la requête dans le filtre, puis émet la réponse : c'est là que les en-têtes partent. */
    private HttpHeaders reponse(MockServerHttpRequest requete, WebFilterChain service) {
        MockServerWebExchange echange = MockServerWebExchange.from(requete);
        filtre.filter(echange, service).then(echange.getResponse().setComplete()).block();
        return echange.getResponse().getHeaders();
    }

    private HttpHeaders reponse(MockServerHttpRequest requete) {
        return reponse(requete, echange -> Mono.empty());
    }

    @Test
    @DisplayName("Toute réponse de l'API porte les en-têtes de sécurité")
    void enTetes_poses() {
        HttpHeaders entetes = reponse(MockServerHttpRequest.get("/amelioration-service/api/v1/nc").build());

        assertThat(entetes.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(entetes.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(entetes.getFirst("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(entetes.getFirst("Permissions-Policy")).contains("camera=()", "geolocation=()");
        assertThat(entetes.getFirst("Content-Security-Policy"))
                .isEqualTo("default-src 'none'; frame-ancestors 'none'");
        assertThat(entetes.getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
    }

    @Test
    @DisplayName("HSTS seulement quand la requête est arrivée en HTTPS, directement ou via le mandataire")
    void hsts_seulementEnHttps() {
        assertThat(reponse(MockServerHttpRequest.get("http://localhost:8088/x").build())
                .containsKey("Strict-Transport-Security"))
                .as("posé sur du HTTP, il empoisonnerait localhost")
                .isFalse();

        assertThat(reponse(MockServerHttpRequest.get("http://passerelle/x")
                .header("X-Forwarded-Proto", "https").build())
                .getFirst("Strict-Transport-Security"))
                .isEqualTo(FiltreEnTetesDeSecurite.HSTS);

        assertThat(reponse(MockServerHttpRequest.get("https://api-gateway.test.qualisira.com/x").build())
                .getFirst("Strict-Transport-Security"))
                .isEqualTo(FiltreEnTetesDeSecurite.HSTS);
    }

    @Test
    @DisplayName("Un en-tête posé par le service aval est respecté")
    void enTeteDuService_respecte() {
        // Un fichier téléchargé peut légitimement se mettre en cache : le service le sait, pas la
        // passerelle.
        HttpHeaders entetes = reponse(MockServerHttpRequest.get("/support-service/api/v1/fichier/1").build(),
                echange -> {
                    echange.getResponse().getHeaders().set(HttpHeaders.CACHE_CONTROL, "private, max-age=60");
                    echange.getResponse().getHeaders().set("X-Frame-Options", "SAMEORIGIN");
                    return Mono.empty();
                });

        assertThat(entetes.get(HttpHeaders.CACHE_CONTROL)).containsExactly("private, max-age=60");
        assertThat(entetes.get("X-Frame-Options")).containsExactly("SAMEORIGIN");
    }

    @Test
    @DisplayName("Une réponse d'authentification n'est jamais mise en cache, quoi qu'en dise le service")
    void authentification_noStoreImpose() {
        HttpHeaders entetes = reponse(MockServerHttpRequest.post("/user-service/api/v1/login").build(),
                echange -> {
                    echange.getResponse().getHeaders().set(HttpHeaders.CACHE_CONTROL, "max-age=3600");
                    return Mono.empty();
                });

        assertThat(entetes.get(HttpHeaders.CACHE_CONTROL)).containsExactly("no-store");
        assertThat(entetes.getFirst(HttpHeaders.PRAGMA)).isEqualTo("no-cache");
    }

    @Test
    @DisplayName("L'interface Swagger reçoit une politique relâchée juste assez pour s'afficher")
    void swagger_politiqueRelachee() {
        HttpHeaders entetes = reponse(MockServerHttpRequest.get("/webjars/swagger-ui/index.html").build());

        assertThat(entetes.getFirst("Content-Security-Policy"))
                .isEqualTo(FiltreEnTetesDeSecurite.POLITIQUE_SWAGGER)
                .contains("frame-ancestors 'none'")
                .doesNotContain("script-src 'self' 'unsafe-inline'");
    }

    @Test
    @DisplayName("Un refus émis par un filtre suivant porte aussi les en-têtes")
    void refus_porteLesEnTetes() {
        HttpHeaders entetes = reponse(MockServerHttpRequest.post("/user-service/api/v1/login").build(),
                echange -> ReponseDeRefus.ecrire(echange,
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "LIMITE", "Trop d'essais."));

        assertThat(entetes.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(entetes.getFirst("Content-Security-Policy")).isEqualTo(FiltreEnTetesDeSecurite.POLITIQUE_API);
    }
}
