package com.qualiapproche.contexte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.contexte", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
public class ContexteApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContexteApplication.class, args);
    }
}
