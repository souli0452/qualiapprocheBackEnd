package com.qualiapproche.userservice.config.auth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class KcAdminConfig {
    private final KcAuthProperties kcAuthProperties;

    @Bean
    public Keycloak keycloak() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ResteasyJackson2Provider jacksonProvider = new ResteasyJackson2Provider();
        jacksonProvider.setMapper(mapper);

        return KeycloakBuilder.builder()
                .serverUrl(kcAuthProperties.getServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(kcAuthProperties.getAdminUsername())
                .password(kcAuthProperties.getAdminPassword())
                .grantType(OAuth2Constants.PASSWORD)
                .resteasyClient(ClientBuilder.newBuilder()
                        .register(jacksonProvider)
                        .build())
                .build();
    }
}
