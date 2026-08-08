package com.qualiapproche.userservice.client;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.StructureDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "referentiel-service", path = "/api/v1")
public interface StructureClient {

    /**
     * État de la licence installée, dont le nombre d'utilisateurs qu'elle autorise.
     *
     * <p>Le point d'entrée est ouvert sans authentification côté référentiel : c'est la passerelle
     * qui l'interroge avant même de résoudre l'utilisateur.</p>
     */
    @GetMapping("/licence/etat")
    EtatLicenceDto etatLicence();

    @GetMapping("/structures/{id}")
    StructureDto getStructureById(@PathVariable("id") UUID id);

    @GetMapping("/structures/direction")
    StructureDto getDirection();

    @GetMapping("/structures/{id}/license-status")
    java.util.Map<String, Object> getStructureLicenseStatus(@PathVariable("id") String id);
}
