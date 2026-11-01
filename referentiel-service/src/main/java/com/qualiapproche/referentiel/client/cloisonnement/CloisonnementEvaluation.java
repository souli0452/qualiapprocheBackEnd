package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "evaluation-service", contextId = "cloisonnementEvaluation", configuration = FeignConfig.class)
public interface CloisonnementEvaluation extends ServiceCloisonne {
}
