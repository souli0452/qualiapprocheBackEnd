package com.qualiapproche.gateway;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * La cloche de l'utilisateur connecté, assemblée depuis tous les modules.
 *
 * <p>Chaque module dit ce qu'il a en attente ; la passerelle les interroge ensemble et rend une
 * seule liste. C'est ici que l'assemblage a sa place : aucun module n'a de raison de connaître les
 * autres, et laisser l'écran les appeler un à un l'aurait rendu responsable de savoir combien il y
 * en a — chaque module ajouté aurait demandé une livraison du front.</p>
 *
 * <p><b>Rien n'est conservé.</b> La liste est recalculée à chaque demande, ce qui la rend juste :
 * un dossier traité entre deux consultations disparaît de lui-même, sans qu'aucun geste n'ait à le
 * marquer comme lu.</p>
 *
 * <p><b>Un module muet ne fait pas taire les autres.</b> Une source injoignable retire ses lignes
 * et la cloche rend ce que les autres savent : une cloche incomplète vaut mieux qu'un écran en
 * erreur, qui se lit comme une panne de l'application entière.</p>
 *
 * <p>Les appels partent en parallèle mais sont rendus dans l'ordre déclaré, pour que la cloche ne
 * change pas d'ordre d'une ouverture à l'autre au gré des temps de réponse.</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
public class NotificationsAggregeesController {

    private static final String COOKIE_JETON = "access_token";

    /** Les modules interrogés, dans l'ordre où leurs lignes se présentent. */
    private static final List<String> SOURCES = List.of(
            "http://AMELIORATION-SERVICE/api/v1/non-conformite/notifications",
            "http://SUPPORT-SERVICE/api/v1/notifications",
            "http://REFERENTIEL-SERVICE/api/v1/notifications");

    /**
     * Au-delà, la source est tenue pour muette. La cloche est consultée d'un geste : la faire
     * attendre le plus lent des modules donnerait l'impression que l'application est figée.
     */
    private static final Duration DELAI = Duration.ofSeconds(3);

    private final WebClient webClient;

    public NotificationsAggregeesController(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.build();
    }

    @GetMapping
    public Mono<Map<String, Object>> notifications(ServerWebExchange exchange) {
        String jeton = jeton(exchange);
        if (jeton == null) {
            // Sans jeton, il n'y a personne à qui répondre. Un 401 viendrait des services eux-mêmes ;
            // l'annoncer ici évite trois appels dont on connaît déjà l'issue.
            return Mono.just(enveloppe(List.of()));
        }

        return Flux.fromIterable(SOURCES)
                .concatMapDelayError(url -> lignesDe(url, jeton))
                .collectList()
                .map(this::enveloppeDesSources);
    }

    /** Les lignes d'un module, ou aucune s'il ne répond pas à temps. */
    private Mono<List<Object>> lignesDe(String url, String jeton) {
        return webClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(DELAI)
                .map(this::extraireLesLignes)
                .onErrorResume(e -> {
                    log.warn("Notifications : {} est muet ({}). Ses lignes sont omises.",
                            url, e.getMessage());
                    return Mono.just(List.of());
                });
    }

    /**
     * Le tableau porté par l'enveloppe des services.
     *
     * <p>La passerelle ne connaît pas la forme d'une ligne, et n'a pas à la connaître : elle
     * transporte ce que le module a rédigé. Ajouter un champ à une notification ne demande donc
     * aucune livraison ici.</p>
     */
    @SuppressWarnings("unchecked")
    private List<Object> extraireLesLignes(Map<?, ?> corps) {
        Object donnees = corps == null ? null : corps.get("data");
        return donnees instanceof List ? (List<Object>) donnees : List.of();
    }

    /** La même enveloppe que celle des services, pour que l'écran n'ait qu'une forme à lire. */
    private Map<String, Object> enveloppeDesSources(List<List<Object>> parSource) {
        List<Object> toutes = new ArrayList<>();
        parSource.forEach(toutes::addAll);
        return enveloppe(toutes);
    }

    private Map<String, Object> enveloppe(List<Object> lignes) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("message", "Opération réussie");
        corps.put("data", lignes);
        corps.put("statusCode", 200);
        return corps;
    }

    private String jeton(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE_JETON);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        // Le cookie est la voie normale, posée à la connexion. L'en-tête reste accepté pour les
        // appels faits hors navigateur — un outil de test, une intégration.
        String entete = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return entete != null && entete.startsWith("Bearer ") ? entete.substring(7) : null;
    }
}
