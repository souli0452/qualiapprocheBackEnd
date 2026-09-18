package com.qualiapproche.ia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.ia", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class IaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IaServiceApplication.class, args);
    }
}
