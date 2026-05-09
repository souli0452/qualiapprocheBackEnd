package com.qualiapproche.amelioration.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
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

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            if (requestTemplate.url().contains("/protocol/openid-connect/token")) {
                return;
            }

            log.info("Interception Feign (Amelioration) : Tentative de récupération du token pour l'appel {}", requestTemplate.url());
            String token = getAccessToken();
            if (token != null) {
                log.info("Token récupéré avec succès.");
                requestTemplate.header("Authorization", "Bearer " + token);
            } else {
                log.error("Échec de la récupération du token.");
            }
        };
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
                log.error("Erreur Keycloak (Amelioration) : Status {} - Body {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception lors de la récupération du token (Amelioration) : {}", e.getMessage());
        }
        return null;
    }
}
