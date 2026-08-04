package com.qualiapproche.support.client;

import com.qualiapproche.common.dto.NiveauConfidentialiteDto;
import com.qualiapproche.support.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Référentiels que support-service consulte sans les détenir.
 *
 * <p>Le point d'entrée {@code /all} est employé plutôt que la liste paginée : celle-ci s'arrête à
 * dix éléments par défaut, et un niveau de confidentialité manquant à l'appel ne restreindrait
 * plus rien — un silence qui s'exercerait précisément sur les documents les plus sensibles.</p>
 */
@FeignClient(name = "referentiel-service", configuration = FeignConfig.class)
public interface ReferentielClient {

    @GetMapping("/api/v1/niveaux-confidentialite/all")
    List<NiveauConfidentialiteDto> niveauxConfidentialite();
}
