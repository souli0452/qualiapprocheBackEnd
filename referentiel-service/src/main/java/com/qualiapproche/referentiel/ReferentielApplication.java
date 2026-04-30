package com.qualiapproche.referentiel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.referentiel", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {
    "com.qualiapproche.referentiel.entities",
    "com.qualiapproche.common.entities"
})
public class ReferentielApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReferentielApplication.class, args);
    }
}
