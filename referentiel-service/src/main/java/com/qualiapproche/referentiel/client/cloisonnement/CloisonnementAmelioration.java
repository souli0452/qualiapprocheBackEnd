package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "amelioration-service", contextId = "cloisonnementAmelioration", configuration = FeignConfig.class)
public interface CloisonnementAmelioration extends ServiceCloisonne {
}
