package com.qualiapproche.workflow;

import com.qualiapproche.common.config.CloisonnementInterneController;
import org.springframework.context.annotation.Import;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableAsync} : sans lui, {@code @Async} est ignoré <b>en silence</b> et l'envoi des
 * courriels d'étape resterait sur le fil de la requête HTTP, la réponse à l'utilisateur attendant
 * le serveur SMTP autant de fois qu'il y a de responsables à prévenir.
 */
@SpringBootApplication
// Le contrôleur interne du cloisonnement vit dans common.config, que ce service ne balaie pas :
// referentiel-service l'appelle pour amorcer un client et rendre les lignes existantes à leur client.
@Import(CloisonnementInterneController.class)
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@EnableAsync
public class WorkflowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowServiceApplication.class, args);
    }
}
