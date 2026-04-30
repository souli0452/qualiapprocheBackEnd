package com.qualiapproche.userservice.config.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KcAuthProperties {
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String issuerUri;
    private String tokenUri;
    private String adminUsername;
    private String adminPassword;
}
