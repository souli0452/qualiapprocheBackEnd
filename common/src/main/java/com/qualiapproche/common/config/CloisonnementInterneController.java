package com.qualiapproche.common.config;

import com.qualiapproche.common.cloisonnement.AmorceDesDirections;
import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.common.cloisonnement.RattrapageDesDirections;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ce que referentiel-service demande à chaque service quand il gère les clients : poser le
 * paramétrage par défaut d'un client, rendre les lignes existantes à leur client.
 *
 * <p>Réservé aux comptes de service : un utilisateur, fût-il administrateur, n'a pas à amorcer un
 * client ni à déplacer des lignes d'une direction à l'autre.</p>
 */
@RestController
@RequestMapping("/api/v1/internal/cloisonnement")
public class CloisonnementInterneController {

    /** Ce que referentiel transmet pour le rattrapage. */
    public record DemandeDeRattrapage(Map<UUID, UUID> structures, UUID parDefaut) {
    }

    private final ObjectProvider<AmorceDesDirections> amorce;
    private final ObjectProvider<RattrapageDesDirections> rattrapage;

    public CloisonnementInterneController(ObjectProvider<AmorceDesDirections> amorce,
                                          ObjectProvider<RattrapageDesDirections> rattrapage) {
        this.amorce = amorce;
        this.rattrapage = rattrapage;
    }

    @PostMapping("/directions/{direction}/amorce")
    public Map<String, Object> amorcer(@PathVariable("direction") UUID direction) {
        exigerUnCompteDeService();
        AmorceDesDirections service = amorce.getIfAvailable();
        List<String> echecs = service == null ? List.of() : service.amorcer(direction);
        return Map.of("direction", direction, "echecs", echecs);
    }

    @PostMapping("/rattrapage")
    public Map<String, Object> rattraper(@RequestBody DemandeDeRattrapage demande) {
        exigerUnCompteDeService();
        RattrapageDesDirections service = rattrapage.getIfAvailable();
        if (service == null || demande == null || demande.structures() == null) {
            return Map.of();
        }
        return service.rattraper(demande.structures(), demande.parDefaut());
    }

    private static void exigerUnCompteDeService() {
        Jwt jwt = SecurityUtils.getJwt();
        if (jwt == null || !Cloisonnement.estUnCompteDeService(jwt)) {
            throw new BusinessException("Réservé aux services de la plateforme.", HttpStatus.FORBIDDEN);
        }
    }
}
