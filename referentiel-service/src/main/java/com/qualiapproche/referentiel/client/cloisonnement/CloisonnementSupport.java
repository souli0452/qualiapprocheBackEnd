package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "support-service", contextId = "cloisonnementSupport", configuration = FeignConfig.class)
public interface CloisonnementSupport extends ServiceCloisonne {
}
