package com.qualiapproche.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
public class ProfileConfig {

    @Configuration
    @Profile("dev")
    @PropertySource(value = {"classpath:config/application.yml", "classpath:config/application-dev.yml"})
    static class Dev {
    }


}
