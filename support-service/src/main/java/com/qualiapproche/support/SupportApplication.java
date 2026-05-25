package com.qualiapproche.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.support", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.qualiapproche.support.client")
@EnableScheduling
public class SupportApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportApplication.class, args);
    }
}
