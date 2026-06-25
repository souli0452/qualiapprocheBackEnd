package com.qualiapproche.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtre global de la Gateway qui lit le cookie {@code access_token}
 * et l'injecte automatiquement dans l'header {@code Authorization: Bearer <token>}
 * pour tous les microservices en aval.
 *
 * <p>Cela permet aux services de continuer à valider le JWT via Spring Security
 * oauth2ResourceServer sans aucune modification de leur côté.
 *
 * <p>Ordre -1 garantit que ce filtre s'exécute avant tous les filtres Gateway.
 */
@Component
public class AuthCookieFilter implements GlobalFilter, Ordered {

    private static final String ACCESS_TOKEN_COOKIE  = "access_token";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();

        HttpCookie accessTokenCookie = cookies.getFirst(ACCESS_TOKEN_COOKIE);

        if (accessTokenCookie != null && !accessTokenCookie.getValue().isBlank()) {
            String token = accessTokenCookie.getValue();

            // Injection de l'Authorization header vers les services en aval
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(AUTHORIZATION_HEADER, "Bearer " + token)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // Pas de cookie : on laisse passer, les services protégés retourneront 401
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Priorité haute : s'exécute avant les filtres de routage Gateway
        return -1;
    }
}
