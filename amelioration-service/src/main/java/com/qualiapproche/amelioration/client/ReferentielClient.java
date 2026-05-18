package com.qualiapproche.amelioration.client;

import com.qualiapproche.amelioration.config.FeignConfig;
import com.qualiapproche.common.dto.ConfigGlobalDto;
import com.qualiapproche.common.dto.StructureDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@FeignClient(name = "referentiel-service", configuration = FeignConfig.class)
public interface ReferentielClient {

    @GetMapping(ROOT_STRUCTURE_API + STRUCTURE_BY_ID)
    StructureDto getStructureById(@PathVariable("id") UUID id);

    @GetMapping(CG_ROOT_URL + GET_ALL_CG)
    ConfigGlobalDto getConfigGlobal();

    @GetMapping(TYPE_PROCESSUS_ROOT_URL + GET_TYPE_PROCESSUS_BY_ID)
    com.qualiapproche.common.dto.TypeProcessusDto getTypeProcessusById(@PathVariable("id") UUID id);
}
