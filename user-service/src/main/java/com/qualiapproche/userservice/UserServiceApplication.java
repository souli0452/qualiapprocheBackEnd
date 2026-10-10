package com.qualiapproche.userservice;

import org.springframework.scheduling.annotation.EnableScheduling;
import com.qualiapproche.userservice.config.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.qualiapproche.userservice", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
