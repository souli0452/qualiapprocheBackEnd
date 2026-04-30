package com.qualiapproche.planification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.planification", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
public class PlanificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanificationApplication.class, args);
    }
}
