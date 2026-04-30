package com.qualiapproche.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String protocol;
    private String auth;
    private String starttlsEnable;
}
