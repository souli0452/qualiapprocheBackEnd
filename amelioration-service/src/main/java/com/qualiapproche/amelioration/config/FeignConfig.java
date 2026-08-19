package com.qualiapproche.amelioration.config;

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
     * Chemins du moteur de workflow réservés aux services : déclarer un fait, redésigner un
     * titulaire. Ils sont appelés depuis des requêtes utilisateur — solder un plan déclare le fait
     * dans la foulée — mais l'acte est celui du <b>module</b>, pas de la personne : le jeton du
     * service part donc toujours, sans quoi le moteur refuserait l'appel.
     *
     * <p>La lecture de l'historique en fait partie : le module la fait pour composer la fiche de
     * clôture, dont le droit d'accès est celui de la non-conformité elle-même. Propager le jeton
     * de l'utilisateur exigerait de lui, en plus, une habilitation de lecture du moteur — et la
     * fiche s'éditerait ou non selon qui la demande.</p>
     */
    private boolean estUnAppelTechnique(String url) {
        if (!url.contains("/instances/")) {
            return false;
        }
        return url.contains("/faits/") || url.contains("/titulaire") || url.contains("/history");
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            if (requestTemplate.url().contains("/protocol/openid-connect/token")) {
                return;
            }

            // Propager le token de l'utilisateur actuel s'il existe — sauf sur les chemins
            // techniques, où c'est l'identité du service qui est attendue.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!estUnAppelTechnique(requestTemplate.url())
                    && authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                log.info("Interception Feign : Propagation du token utilisateur pour {}", requestTemplate.url());
                requestTemplate.header("Authorization", "Bearer " + jwtAuthenticationToken.getToken().getTokenValue());
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
