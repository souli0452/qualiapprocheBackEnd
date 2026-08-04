package com.qualiapproche.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le filtre est le seul endroit où les permissions applicatives (AppRole, connues de la seule base
 * de user-service) rejoignent la requête : {@code PermissionChecker}, en aval, ne les lit que dans
 * {@code X-User-Permissions}.
 *
 * <p>D'où le risque que ces tests verrouillent : le filtre ne résolvait l'en-tête que pour les
 * routes {@code /support-service/}, du temps où ce service portait seul {@code @RequirePermissions}.
 * Les contrôleurs protégés depuis (workflow, referentiel, amelioration, user) recevaient donc une
 * requête sans en-tête, et refusaient <b>tous</b> leurs appels — y compris ceux du SUPER_ADMIN, qui
 * détient pourtant chaque permission du dictionnaire. Rien ne distinguait cette panne d'une
 * permission manquante : ni la compilation, ni le démarrage, ni le message d'erreur.</p>
 */
class AuthCookieFilterTest {

    private static final String JETON = "jeton-de-test";
    private static final String REPONSE_PERMISSIONS =
            "{\"data\":[\"workflow-write\",\"workflow-read\"],\"message\":\"OK\",\"statusCode\":200}";

    private final AtomicInteger appelsUserService = new AtomicInteger();
    private final AtomicReference<ServerHttpRequest> requeteAval = new AtomicReference<>();

    private final GatewayFilterChain chaine = exchange -> {
        requeteAval.set(exchange.getRequest());
        return Mono.empty();
    };

    /**
     * Un {@code WebClient} dont la fonction d'échange est stubée : ni Eureka ni réseau, mais le
     * filtre passe bien par le chemin nominal (URI, en-tête Authorization, lecture du corps).
     */
    private AuthCookieFilter filtre(long ttlSecondes) {
        ExchangeFunction echange = requete -> {
            appelsUserService.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(REPONSE_PERMISSIONS)
                    .build());
        };
        return new AuthCookieFilter(
                WebClient.builder().exchangeFunction(echange),
                new UserPermissionsCache(ttlSecondes));
    }

    private void filtrer(AuthCookieFilter filtre, MockServerHttpRequest requete) {
        filtre.filter(MockServerWebExchange.from(requete), chaine).block();
    }

    private MockServerHttpRequest authentifiee(String chemin) {
        return MockServerHttpRequest.get(chemin)
                .cookie(new HttpCookie("access_token", JETON))
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/workflow-service/api/v1/workflows/3f2504e0-4f89-11d3-9a0c-0305e82c3301",
            "/referentiel-service/api/v1/structures",
            "/amelioration-service/api/v1/non-conformites",
            "/user-service/api/v1/roles",
            "/support-service/api/v1/documents"
    })
    @DisplayName("Les permissions sont résolues pour toute route, pas pour le seul support-service")
    void permissions_resoluesPourToutesLesRoutes(String chemin) {
        filtrer(filtre(60), authentifiee(chemin));

        assertThat(requeteAval.get().getHeaders().getFirst("X-User-Permissions"))
                .withFailMessage("Sans X-User-Permissions sur %s, PermissionChecker ne voit que les "
                        + "rôles techniques du JWT et refuse tout — SUPER_ADMIN compris.", chemin)
                .isEqualTo("workflow-write,workflow-read");
        assertThat(requeteAval.get().getHeaders().getFirst("Authorization"))
                .isEqualTo("Bearer " + JETON);
    }

    @Test
    @DisplayName("Un X-User-Permissions envoyé par le client est écrasé, jamais propagé")
    void enTeteForgeParLeClient_estEcrase() {
        filtrer(filtre(60), MockServerHttpRequest.get("/workflow-service/api/v1/workflows")
                .cookie(new HttpCookie("access_token", JETON))
                .header("X-User-Permissions", "workflow-write,document-write")
                .build());

        // La valeur retenue est celle résolue auprès de user-service, pas celle du client.
        assertThat(requeteAval.get().getHeaders().get("X-User-Permissions"))
                .containsExactly("workflow-write,workflow-read");
    }

    @Test
    @DisplayName("Sans cookie, aucune permission n'est posée ni même résolue")
    void sansCookie_aucuneResolution() {
        filtrer(filtre(60), MockServerHttpRequest.get("/workflow-service/api/v1/workflows")
                .header("X-User-Permissions", "workflow-write")
                .build());

        assertThat(requeteAval.get().getHeaders().get("X-User-Permissions")).isNull();
        assertThat(requeteAval.get().getHeaders().getFirst("Authorization")).isNull();
        assertThat(appelsUserService).hasValue(0);
    }

    @Test
    @DisplayName("Le cache évite un appel à user-service par requête")
    void resolutionMiseEnCache_unSeulAppelPourUnMemeJeton() {
        AuthCookieFilter filtre = filtre(60);

        filtrer(filtre, authentifiee("/workflow-service/api/v1/workflows"));
        filtrer(filtre, authentifiee("/referentiel-service/api/v1/structures"));
        filtrer(filtre, authentifiee("/amelioration-service/api/v1/risques"));

        assertThat(appelsUserService)
                .withFailMessage("Étendre la résolution à toutes les routes ne doit pas se payer "
                        + "d'un appel à user-service par requête entrante.")
                .hasValue(1);
        assertThat(requeteAval.get().getHeaders().getFirst("X-User-Permissions"))
                .isEqualTo("workflow-write,workflow-read");
    }

    @Test
    @DisplayName("user-service indisponible : la requête passe, sans permissions et sans cache")
    void userServiceIndisponible_neFigePasUnResultatVide() {
        AtomicInteger appels = new AtomicInteger();
        AuthCookieFilter filtre = new AuthCookieFilter(
                WebClient.builder().exchangeFunction(requete -> {
                    appels.incrementAndGet();
                    return Mono.error(new IllegalStateException("user-service injoignable"));
                }),
                new UserPermissionsCache(60));

        filtrer(filtre, authentifiee("/workflow-service/api/v1/workflows"));
        assertThat(requeteAval.get().getHeaders().getFirst("X-User-Permissions")).isEmpty();

        // Le second appel doit retenter : mettre en cache un échec priverait l'utilisateur de ses
        // permissions pendant toute la durée de vie de l'entrée.
        filtrer(filtre, authentifiee("/workflow-service/api/v1/workflows"));
        assertThat(appels).hasValue(2);
    }
}
