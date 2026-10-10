package com.qualiapproche.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.OrderComparator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebFilter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.all;

/**
 * Les quatre filtres ensemble, dans l'ordre où Spring les range, devant un service factice.
 *
 * <p>Chacun est testé seul ailleurs ; ce qui se vérifie ici, c'est ce que leur ordre produit et
 * qu'aucun test isolé ne voit : un 429 qui porte les en-têtes CORS (sinon le navigateur le masque),
 * un refus d'origine motivé plutôt que le 403 muet de CORS, un contrôle préalable qui aboutit.</p>
 */
class ChaineDesFiltresWebTest {

    private static final String FRONTAL = "https://app.test.qualisira.com";

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        CorsConfig cors = new CorsConfig();
        ReflectionTestUtils.setField(cors, "origines", "");
        ReflectionTestUtils.setField(cors, "motifsOrigines", CorsConfig.MOTIFS_PAR_DEFAUT);
        CorsConfiguration configuration = cors.configurationCors();

        FiltreLimiteDeDebit limite = new FiltreLimiteDeDebit();
        FiltreLimiteDeDebit.Regle connexion = new FiltreLimiteDeDebit.Regle();
        connexion.setChemins(List.of("/user-service/api/v1/login"));
        connexion.setCapacite(2);
        connexion.setPeriode(Duration.ofMinutes(1));
        limite.setRegles(Map.of("connexion", connexion));

        // Rangés comme Spring les range : par leur ordre déclaré, et non par l'ordre d'écriture.
        List<WebFilter> filtres = new ArrayList<>(List.of(
                limite, cors.corsWebFilter(configuration), new FiltreAntiCsrf(configuration),
                new FiltreEnTetesDeSecurite()));
        filtres.sort(OrderComparator.INSTANCE);

        WebTestClient.RouterFunctionSpec spec = WebTestClient.bindToRouterFunction(
                RouterFunctions.route(all(), requete -> ServerResponse.ok().bodyValue("{}")));
        filtres.forEach(spec::webFilter);
        // Une adresse complète, comme en vrai : sans hôte, Spring ne sait pas dire si la requête
        // est inter-origines et le filtre CORS la refuse d'office.
        client = spec.configureClient().baseUrl("https://api-gateway.test.qualisira.com").build();
    }

    @Test
    @DisplayName("Le frontal connu écrit, et reçoit les en-têtes CORS et de sécurité")
    void frontalConnu_ecrit() {
        client.post().uri("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, FRONTAL)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTAL)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY");
    }

    @Test
    @DisplayName("Un site étranger reçoit un 403 motivé, sans en-tête CORS")
    void siteEtranger_refuse() {
        client.post().uri("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://site-piege.fr")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectBody().jsonPath("$.motif").isEqualTo("ORIGINE");
    }

    @Test
    @DisplayName("Un site étranger ne peut pas lire une réponse : CORS ne lui renvoie pas son origine")
    void siteEtranger_lectureNonAutorisee() {
        client.get().uri("/amelioration-service/api/v1/nc")
                .header(HttpHeaders.ORIGIN, "https://site-piege.fr")
                .exchange()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    @DisplayName("Le contrôle préalable du frontal aboutit")
    void controlePrealable_aboutit() {
        client.options().uri("/user-service/api/v1/login")
                .header(HttpHeaders.ORIGIN, FRONTAL)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTAL);
    }

    @Test
    @DisplayName("Le 429 de la limitation porte les en-têtes CORS : le frontal peut le lire")
    void limite_429LisibleParLeFrontal() {
        for (int i = 0; i < 2; i++) {
            client.post().uri("/user-service/api/v1/login").header(HttpHeaders.ORIGIN, FRONTAL)
                    .exchange().expectStatus().isOk();
        }

        client.post().uri("/user-service/api/v1/login")
                .header(HttpHeaders.ORIGIN, FRONTAL)
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectHeader().exists("Retry-After")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTAL)
                .expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "no-store")
                .expectBody().jsonPath("$.motif").isEqualTo("LIMITE");
    }

    @Test
    @DisplayName("Un appel sans origine (entre services, script) passe")
    void sansOrigine_passe() {
        client.post().uri("/workflow-service/api/v1/instances")
                .exchange()
                .expectStatus().isOk();
    }
}
