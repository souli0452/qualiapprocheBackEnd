package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "ia-service", contextId = "cloisonnementIa", configuration = FeignConfig.class)
public interface CloisonnementIa extends ServiceCloisonne {
}
