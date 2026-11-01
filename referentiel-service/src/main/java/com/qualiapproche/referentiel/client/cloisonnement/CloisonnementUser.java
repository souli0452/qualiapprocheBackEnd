package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", contextId = "cloisonnementUser", configuration = FeignConfig.class)
public interface CloisonnementUser extends ServiceCloisonne {
}
