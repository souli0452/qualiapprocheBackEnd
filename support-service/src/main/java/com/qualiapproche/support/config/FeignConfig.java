package com.qualiapproche.support.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

            // Propager le token de l'utilisateur actuel s'il existe
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                log.info("Interception Feign : Propagation du token utilisateur pour {}", requestTemplate.url());
                requestTemplate.header("Authorization", "Bearer " + jwtAuthenticationToken.getToken().getTokenValue());
                return;
            }

            // Sinon, utiliser le client credentials (pour les tâches de fond ou si pas de contexte utilisateur)
            if (requestTemplate.url().contains("/protocol/openid-connect/token")) {
                return;
            }

            log.info("Interception Feign : Tentative de récupération du token client_credentials pour {}", requestTemplate.url());
            String token = getAccessToken();
            if (token != null) {
                requestTemplate.header("Authorization", "Bearer " + token);
            } else {
                log.error("Échec de la récupération du token pour l'appel Feign.");
            }
        };
    }

    @Bean
    public Decoder feignDecoder(@Autowired ObjectMapper objectMapper) {
        return new ApiResponseFeignDecoder(objectMapper);
    }

    private String getAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "client_credentials");
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            } else {
                log.error("Erreur Keycloak lors de la récupération du token : Status {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Exception lors de la récupération du token : {}", e.getMessage());
        }
        return null;
    }
}
