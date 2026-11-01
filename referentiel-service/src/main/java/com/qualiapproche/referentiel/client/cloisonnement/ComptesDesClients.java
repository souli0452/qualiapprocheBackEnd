package com.qualiapproche.referentiel.client.cloisonnement;

import com.qualiapproche.common.config.CloisonnementInterneController.DemandeDeRattrapage;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** Les comptes Keycloak, que user-service tient hors de toute base : leur part de la gestion des clients. */
@FeignClient(name = "user-service", contextId = "comptesDesClients", configuration = FeignConfig.class)
public interface ComptesDesClients {

    /** Pose l'attribut de direction sur les comptes qui n'en ont pas. */
    @PostMapping("/api/v1/internal/plateforme/comptes/rattrapage")
    Map<String, Object> rattraper(@RequestBody DemandeDeRattrapage demande);

    /** Crée un administrateur de la plateforme : sans direction, rôle de royaume PLATEFORME_ADMIN. */
    @PostMapping("/api/v1/internal/plateforme/administrateurs")
    Object creerAdministrateurDePlateforme(@RequestBody KcUserDto compte);
}
