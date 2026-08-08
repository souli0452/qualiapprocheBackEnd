package com.qualiapproche.referentiel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.referentiel", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@ComponentScan(basePackages = {"com.qualiapproche.referentiel", "com.qualiapproche.common"})
@EntityScan(basePackages = {
    "com.qualiapproche.referentiel.entities",
    "com.qualiapproche.common.entities"
})
public class ReferentielApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReferentielApplication.class, args);
    }
}
