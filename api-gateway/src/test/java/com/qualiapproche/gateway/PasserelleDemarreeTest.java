package com.qualiapproche.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La passerelle réellement démarrée, avec son {@code application.yml}.
 *
 * <p>Les tests des filtres les construisent à la main ; celui-ci vérifie ce qu'ils ne voient pas :
 * que Spring les assemble (une seule configuration CORS, partagée), que les seuils et les origines
 * du fichier de configuration sont bien lus, et que Swagger est coupé par défaut.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class PasserelleDemarreeTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private CorsConfiguration configurationCors;

    @Autowired
    private FiltreLimiteDeDebit limite;

    @Test
    @DisplayName("Les origines par défaut sont celles du produit, sans joker")
    void originesParDefaut() {
        assertThat(configurationCors.getAllowedOriginPatterns())
                .doesNotContain("*")
                .contains("https://app.test.qualisira.com")
                .doesNotContain("https://qualisira.test.qualisira.com", "https://qualisira.horeb.tech")
                .noneMatch(motif -> motif.contains("*.horeb.tech") || motif.contains("*.qualisira.com"));
    }

    @Test
    @DisplayName("Les règles de limitation du fichier de configuration sont lues")
    void reglesLues() {
        assertThat(limite.getRegles()).containsKeys("connexion", "rafraichissement",
                "reinitialisation", "mot-de-passe");
        assertThat(limite.getRegles().get("connexion").getChemins())
                .containsExactly("/user-service/api/v1/login");
        assertThat(limite.getSautsDeConfiance()).isEqualTo(1);
    }

    @Test
    @DisplayName("Une écriture depuis un site étranger est refusée avant tout routage, en-têtes posés")
    void ecritureEtrangere_refusee() {
        client.post().uri("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://site-piege.fr")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Content-Security-Policy", FiltreEnTetesDeSecurite.POLITIQUE_API)
                .expectBody().jsonPath("$.motif").isEqualTo("ORIGINE");
    }

    @Test
    @DisplayName("L'interface Swagger agrégée est coupée par défaut")
    void swagger_coupe() {
        client.get().uri("/v3/api-docs").exchange().expectStatus().isNotFound();
        client.get().uri("/webjars/swagger-ui/index.html").exchange().expectStatus().isNotFound();
    }
}
