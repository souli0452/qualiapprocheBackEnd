package com.qualiapproche.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Filtre global de la Gateway qui lit le cookie {@code access_token} et l'injecte automatiquement
 * dans l'header {@code Authorization: Bearer <token>} pour tous les microservices en aval.
 *
 * <p>Il interroge en plus {@code user-service} pour résoudre les permissions applicatives
 * (AppRole, stockées uniquement dans la base de user-service) de l'utilisateur connecté, et les
 * transmet via l'en-tête interne {@code X-User-Permissions}. Toute valeur de cet en-tête envoyée
 * par le client d'origine est systématiquement écrasée : elle ne doit jamais venir d'ailleurs que
 * de la gateway elle-même. Le résultat est mis en cache quelques dizaines de secondes
 * ({@link UserPermissionsCache}) pour ne pas appeler user-service à chaque requête.
 *
 * <p>Cette résolution vaut pour <b>toutes</b> les routes, et non plus pour le seul support-service.
 * La restriction datait du temps où il était l'unique porteur de {@code @RequirePermissions} ;
 * depuis, workflow-service, referentiel-service, amelioration-service et user-service en portent
 * aussi. Sans en-tête, {@code PermissionChecker} ne voit que les rôles techniques du JWT et refuse
 * l'accès à tout le monde — y compris au SUPER_ADMIN, qui détient pourtant la permission en base :
 * l'administration des circuits de validation était ainsi devenue impossible à qui que ce soit.
 * Un préfixe à déclarer ici à chaque nouveau service protégé serait le même piège, silencieux et
 * indiscernable d'une permission manquante.
 *
 * <p>Ordre -1 garantit que ce filtre s'exécute avant tous les filtres Gateway.
 */
@Slf4j
@Component
public class AuthCookieFilter implements GlobalFilter, Ordered {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String PERMISSIONS_HEADER = "X-User-Permissions";

    private final WebClient webClient;
    private final UserPermissionsCache permissionsCache;

    public AuthCookieFilter(WebClient.Builder loadBalancedWebClientBuilder, UserPermissionsCache permissionsCache) {
        this.webClient = loadBalancedWebClientBuilder.build();
        this.permissionsCache = permissionsCache;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
        HttpCookie accessTokenCookie = cookies.getFirst(ACCESS_TOKEN_COOKIE);

        // Jamais faire confiance à un X-User-Permissions envoyé par le client : toujours écrasé.
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(PERMISSIONS_HEADER));

        if (accessTokenCookie == null || accessTokenCookie.getValue().isBlank()) {
            // Pas de cookie : on laisse passer, les services protégés retourneront 401
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        String token = accessTokenCookie.getValue();
        requestBuilder.header(AUTHORIZATION_HEADER, "Bearer " + token);

        Optional<String[]> cached = permissionsCache.get(token);
        if (cached.isPresent()) {
            log.debug("Permissions (cache) pour {} : {}", exchange.getRequest().getURI().getPath(), Arrays.toString(cached.get()));
            requestBuilder.header(PERMISSIONS_HEADER, String.join(",", cached.get()));
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        return webClient.get()
                .uri("http://USER-SERVICE/api/v1/me/permissions")
                .header(AUTHORIZATION_HEADER, "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(3))
                .map(this::extractPermissions)
                .defaultIfEmpty(new String[0])
                .doOnNext(permissions -> log.info("Permissions résolues via user-service pour {} : {}",
                        exchange.getRequest().getURI().getPath(), Arrays.toString(permissions)))
                // Uniquement en cas de succès : une erreur (ex. user-service momentanément indisponible)
                // ne doit pas figer un résultat vide en cache pour toute sa durée de vie.
                .doOnNext(permissions -> permissionsCache.put(token, permissions))
                .onErrorResume(e -> {
                    log.error("Échec de la récupération des permissions auprès de user-service pour {} : {}",
                            exchange.getRequest().getURI().getPath(), e.toString());
                    return Mono.just(new String[0]);
                })
                .flatMap(permissions -> {
                    requestBuilder.header(PERMISSIONS_HEADER, String.join(",", permissions));
                    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                });
    }

    /**
     * user-service enveloppe ses réponses dans {@code {"data": [...], "message": ..., "statusCode": ...}}
     * (GlobalResponseHandler, module common). On lit donc {@code data} ; si jamais l'endpoint
     * répond un jour un tableau brut, on le prend aussi tel quel.
     */
    private String[] extractPermissions(JsonNode json) {
        JsonNode arrayNode = json.isArray() ? json : json.get("data");
        if (arrayNode == null || !arrayNode.isArray()) {
            return new String[0];
        }
        String[] result = new String[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            result[i] = arrayNode.get(i).asText();
        }
        return result;
    }

    @Override
    public int getOrder() {
        // Priorité haute : s'exécute avant les filtres de routage Gateway
        return -1;
    }
}
