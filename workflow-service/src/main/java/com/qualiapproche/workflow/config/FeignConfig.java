package com.qualiapproche.workflow.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Propagation de l'identité sur les appels sortants du service de workflow.
 *
 * <p>Les webhooks émis vers {@code support-service} et {@code amelioration-service} partaient
 * jusqu'ici sans en-tête {@code Authorization}, alors que leurs points de rappel
 * ({@code /api/v1/internal/callbacks/**}) sont protégés par {@code anyRequest().authenticated()} :
 * chaque notification se terminait par un 401 avalé par le {@code catch} appelant, et le statut
 * du document ou de la non-conformité n'était jamais mis à jour.</p>
 *
 * <p>Le jeton de l'utilisateur courant est propagé quand il existe ; les notifications émises
 * hors requête HTTP entrante retombent sur un jeton {@code client_credentials}, comme le font
 * déjà support-service, amelioration-service et referentiel-service.</p>
 */
@Slf4j
@Configuration
public class FeignConfig {

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUri;

    /**
     * En-tête des permissions applicatives, propagé aux appels entre services.
     *
     * <p>La gateway résout ces permissions une fois par requête entrante et les transmet dans
     * {@code X-User-Permissions} ; le jeton Keycloak, lui, ne porte que des rôles techniques.
     * Un appel de service à service ne passant pas par la gateway, l'en-tête se perdait en
     * chemin : le service appelé ne voyait plus aucune permission applicative et refusait tout
     * point d'entrée protégé par {@code @perm}.</p>
     *
     * <p>Propager la valeur reçue ne l'expose pas : la gateway écrase systématiquement tout
     * {@code X-User-Permissions} venu du client. Ce qui circule ici est donc ce qu'elle a
     * elle-même établi.</p>
     */
    private static final String PERMISSIONS_HEADER = "X-User-Permissions";

    private void propagerLesPermissions(feign.RequestTemplate requestTemplate) {
        var attributs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (!(attributs instanceof org.springframework.web.context.request.ServletRequestAttributes servlet)) {
            return;
        }
        String permissions = servlet.getRequest().getHeader(PERMISSIONS_HEADER);
        if (permissions != null && !permissions.isBlank()) {
            requestTemplate.header(PERMISSIONS_HEADER, permissions);
        }
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            propagerLesPermissions(requestTemplate);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                requestTemplate.header("Authorization", "Bearer " + jwtAuthenticationToken.getToken().getTokenValue());
                return;
            }

            if (requestTemplate.url().contains("/protocol/openid-connect/token")) {
                return;
            }

            String token = getAccessToken();
            if (token != null) {
                requestTemplate.header("Authorization", "Bearer " + token);
            } else {
                log.error("Échec de la récupération du jeton de service pour l'appel Feign {}", requestTemplate.url());
            }
        };
    }

    private String getAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            log.error("Erreur Keycloak lors de la récupération du jeton de service : status {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Exception lors de la récupération du jeton de service : {}", e.getMessage());
        }
        return null;
    }
}
