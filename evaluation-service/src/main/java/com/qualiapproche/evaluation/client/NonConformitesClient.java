package com.qualiapproche.evaluation.client;

import com.qualiapproche.common.dto.NonConformiteDAuditDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.evaluation.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Le module Non-conformités (M05), vu de l'audit : on n'y fait qu'ouvrir des dossiers. */
@FeignClient(name = "amelioration-service", configuration = FeignConfig.class)
public interface NonConformitesClient {

    /** Ouvre — ou retrouve, pour une clé déjà transmise — la non-conformité d'un écart d'audit. */
    @PostMapping("/api/v1/non-conformite/depuis-audit")
    NonConformiteDto transmettre(@RequestBody NonConformiteDAuditDto ecart);
}
