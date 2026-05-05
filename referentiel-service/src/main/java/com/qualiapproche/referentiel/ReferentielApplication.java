package com.qualiapproche.referentiel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.referentiel", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@EnableFeignClients
@org.springframework.scheduling.annotation.EnableScheduling
@ComponentScan(basePackages = {"com.qualiapproche.referentiel", "com.qualiapproche.common"})
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {
    "com.qualiapproche.referentiel.entities",
    "com.qualiapproche.common.entities"
})
public class ReferentielApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReferentielApplication.class, args);
    }
}
