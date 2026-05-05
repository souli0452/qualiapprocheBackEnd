package com.qualiapproche.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.userservice", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@org.springframework.cloud.openfeign.EnableFeignClients
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
