package com.qualiapproche.evaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Évaluation et conduite des audits (module M13).
 *
 * <p>Le service tient le programme d'audit ; le suivi des échéances tourne chaque nuit
 * (voir {@code SurveillanceDesEcheances}), d'où l'ordonnanceur.</p>
 */
@SpringBootApplication(scanBasePackages = {"com.qualiapproche.evaluation", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class EvaluationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvaluationApplication.class, args);
    }
}
