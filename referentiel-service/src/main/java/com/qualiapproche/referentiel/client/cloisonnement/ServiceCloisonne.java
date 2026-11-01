package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.common.config.CloisonnementInterneController.DemandeDeRattrapage;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

/**
 * Ce que chaque service expose pour la gestion des clients ({@code CloisonnementInterneController}) :
 * amorcer le paramétrage d'un client, rendre les lignes existantes à leur client.
 */
public interface ServiceCloisonne {

    @PostMapping("/api/v1/internal/cloisonnement/directions/{direction}/amorce")
    Map<String, Object> amorcer(@PathVariable("direction") UUID direction);

    @PostMapping("/api/v1/internal/cloisonnement/rattrapage")
    Map<String, Object> rattraper(@RequestBody DemandeDeRattrapage demande);
}
