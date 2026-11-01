package com.qualiapproche.userservice.controller;

import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.userservice.service.KcUserService;
import com.qualiapproche.userservice.service.RattrapageDesComptes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Ce que referentiel-service demande à user-service quand il gère les clients : créer un
 * administrateur de la plateforme, rattacher les comptes existants à leur client.
 *
 * <p>Réservé aux comptes de service : la gestion des clients passe par referentiel, qui vérifie
 * que le demandeur est administrateur de la plateforme.</p>
 */
@RestController
@RequestMapping("/api/v1/internal/plateforme")
@RequiredArgsConstructor
public class PlateformeInterneController {

    /** Correspondance structure → direction racine, et direction des comptes orphelins. */
    public record DemandeDeRattachement(Map<UUID, UUID> structures, UUID parDefaut) {
    }

    private final KcUserService comptes;
    private final RattrapageDesComptes rattrapage;

    @PostMapping("/administrateurs")
    public KcUserDto creerUnAdministrateur(@RequestBody KcUserDto demande) {
        exigerUnCompteDeService();
        return comptes.creerAdministrateurDePlateforme(demande);
    }

    @PostMapping("/comptes/rattrapage")
    public Map<String, Object> rattacherLesComptes(@RequestBody DemandeDeRattachement demande) {
        exigerUnCompteDeService();
        return rattrapage.rattacher(demande == null ? null : demande.structures(),
                demande == null ? null : demande.parDefaut());
    }

    private static void exigerUnCompteDeService() {
        Jwt jwt = SecurityUtils.getJwt();
        if (jwt == null || !Cloisonnement.estUnCompteDeService(jwt)) {
            throw new BusinessException("Réservé aux services de la plateforme.", HttpStatus.FORBIDDEN);
        }
    }
}
