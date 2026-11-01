package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "workflow-service", contextId = "cloisonnementWorkflow", configuration = FeignConfig.class)
public interface CloisonnementWorkflow extends ServiceCloisonne {
}
