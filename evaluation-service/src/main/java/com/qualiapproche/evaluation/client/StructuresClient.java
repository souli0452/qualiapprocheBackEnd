package com.qualiapproche.evaluation.client;

import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.evaluation.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.ROOT_STRUCTURE_API;
import static com.qualiapproche.common.utils.ApiUrls.STRUCTURE_BY_ID;

/** Les structures du référentiel : le processus audité en est une. */
@FeignClient(name = "referentiel-service", configuration = FeignConfig.class)
public interface StructuresClient {

    @GetMapping(ROOT_STRUCTURE_API + STRUCTURE_BY_ID)
    StructureDto structure(@PathVariable("id") UUID id);
}
