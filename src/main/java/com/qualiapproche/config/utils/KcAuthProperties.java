package com.qualiapproche.config.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "kc.auth")
public class KcAuthProperties {
    private String realm;
    private String clientId;
    private String clientSecret;
    private String scope;
    private String tokenUri;
    private String issuerUri;
    private String serverUrl;
    private String adminUsername;
    private String adminPassword;
}
