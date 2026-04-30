package com.qualiapproche.amelioration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
    "com.qualiapproche.amelioration",
    "com.qualiapproche.common.config"
})
@EnableDiscoveryClient
@EnableFeignClients
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = {
    "com.qualiapproche.amelioration.repository",
    "com.qualiapproche.referentiel.repository"
})
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {
    "com.qualiapproche.amelioration.entities",
    "com.qualiapproche.referentiel.entities",
    "com.qualiapproche.common.entities"
})

public class AmeliorationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmeliorationApplication.class, args);
    }
}
