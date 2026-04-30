package com.qualiapproche.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.support", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
public class SupportApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportApplication.class, args);
    }
}
