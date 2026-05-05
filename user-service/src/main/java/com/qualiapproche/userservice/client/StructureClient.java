package com.qualiapproche.userservice.client;

import com.qualiapproche.common.dto.StructureDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "referentiel-service", path = "/api/v1")
public interface StructureClient {

    @GetMapping("/structures/{id}")
    StructureDto getStructureById(@PathVariable("id") UUID id);

    @GetMapping("/structures/direction")
    StructureDto getDirection();

    @GetMapping("/structures/{id}/license-status")
    java.util.Map<String, Object> getStructureLicenseStatus(@PathVariable("id") String id);
}
