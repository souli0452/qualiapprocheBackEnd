package com.qualiapproche.amelioration;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.qualiapproche.amelioration",
        "com.qualiapproche.common.config"
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaRepositories(basePackages = {
        "com.qualiapproche.amelioration.repository",
        "com.qualiapproche.amelioration.courriel"
})
@EntityScan(basePackages = {
        "com.qualiapproche.amelioration.entities",
        "com.qualiapproche.amelioration.courriel",
        "com.qualiapproche.common.entities"
})

public class AmeliorationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmeliorationApplication.class, args);
    }
}
