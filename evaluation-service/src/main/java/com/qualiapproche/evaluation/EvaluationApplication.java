package com.qualiapproche.evaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.qualiapproche.evaluation", "com.qualiapproche.common.config"})
@EnableDiscoveryClient
public class EvaluationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvaluationApplication.class, args);
    }
}
