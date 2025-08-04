package com.qualiapproche.config.auth;
import com.qualiapproche.config.utils.KcAuthProperties;
import lombok.RequiredArgsConstructor;
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
        return KeycloakBuilder.builder()
                .serverUrl(kcAuthProperties.getServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(kcAuthProperties.getAdminUsername())
                .password(kcAuthProperties.getAdminPassword())
                .grantType(OAuth2Constants.PASSWORD)
                .build();
    }
}
