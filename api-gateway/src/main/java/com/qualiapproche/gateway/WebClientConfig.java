package com.qualiapproche.gateway;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Client réactif résolu via Eureka (les URI {@code http://<SERVICE-ID>/...} sont load-balancées,
     * comme les routes {@code lb://} de la gateway). Utilisé par {@link AuthCookieFilter} pour
     * interroger user-service.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
