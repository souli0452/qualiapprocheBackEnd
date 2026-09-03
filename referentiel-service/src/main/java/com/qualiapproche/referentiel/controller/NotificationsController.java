package com.qualiapproche.referentiel.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.NotificationDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.common.enumeration.SourceNotification;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ce que l'utilisateur connecté a en attente du côté du référentiel : à ce jour, la licence.
 *
 * <p>Recalculé à chaque appel, jamais conservé. La licence s'y prête particulièrement : son état
 * est une date confrontée au jour présent, et une ligne persistée aurait continué d'annoncer une
 * expiration après le renouvellement.</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Ce que l'utilisateur connecté a en attente")
public class NotificationsController {

    public static final String CODE_LICENCE_EXPIREE = "LICENCE_EXPIREE";
    public static final String CODE_LICENCE_BIENTOT_EXPIREE = "LICENCE_BIENTOT_EXPIREE";

    /** Rôle qui peut renouveler : le même que celui que la relance quotidienne prévient. */
    private static final String ROLE_ADMIN = "SUPER_ADMIN";

    private final LicenceInstalleeService licenceInstalleeService;

    /** Derniers jours, où le terme devient pressant. */
    @Value("${qualisira.licence.preavis-jours:3}")
    private int preavisJours;

    /**
     * Fenêtre d'annonce. La relance par courriel ne part qu'à des jours précis ; la cloche, elle,
     * reste allumée tant que le terme approche — sans quoi elle serait muette les jours creux,
     * ce qui se lit comme « rien à signaler ».
     */
    @Value("${qualisira.licence.rappels-jalons:30,15}")
    private String jalons;

    /**
     * La ligne de la licence, s'il y a lieu.
     *
     * <p>Réservée à qui peut agir. La relance quotidienne ne prévient que l'administration, pour la
     * même raison : annoncer une échéance de licence à un agent qui ne peut ni la renouveler ni la
     * saisir n'est pas une notification, c'est du bruit dans une cloche destinée à son travail.</p>
     *
     * <p>Enveloppé explicitement : {@code GlobalResponseHandler} pagine d'office toute réponse de
     * type {@code List}.</p>
     */
    @Operation(summary = "Notifications de licence, pour qui peut la renouveler")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> notifications() {
        if (!SecurityUtils.hasRole(ROLE_ADMIN)) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        EtatLicenceDto licence;
        try {
            licence = licenceInstalleeService.etat();
        } catch (Exception e) {
            log.warn("État de la licence indisponible pour la cloche : {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        if (licence == null || "ABSENTE".equals(licence.getStatut())) {
            // Aucune licence installée : l'écran dédié le dit déjà, et le redire ici à chaque
            // ouverture de la cloche n'apprendrait rien de nouveau.
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        long restants = licence.getJoursRestants();
        if (restants < 0) {
            return ResponseEntity.ok(ApiResponse.success(List.of(NotificationDto.builder()
                    .source(SourceNotification.LICENCE)
                    .code(CODE_LICENCE_EXPIREE)
                    .titre("Licence expirée")
                    .detail(detail(licence, "Les écritures sont suspendues."))
                    .gravite(GraviteNotification.URGENT)
                    .nombre(1)
                    .build())));
        }

        if (restants > fenetreDAnnonce()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        return ResponseEntity.ok(ApiResponse.success(List.of(NotificationDto.builder()
                .source(SourceNotification.LICENCE)
                .code(CODE_LICENCE_BIENTOT_EXPIREE)
                .titre("Licence à renouveler")
                .detail(detail(licence, restants == 0
                        ? "Elle prend fin aujourd'hui."
                        : restants == 1
                                ? "Il reste un jour."
                                : "Il reste " + restants + " jours."))
                .gravite(restants <= preavisJours ? GraviteNotification.URGENT : GraviteNotification.ATTENTION)
                .nombre(1)
                .build())));
    }

    /**
     * La phrase du service dit quoi faire ; à défaut, celle calculée ici dit au moins où l'on en
     * est. Les deux valent mieux qu'un intitulé seul, qui laisserait chercher l'échéance.
     */
    private String detail(EtatLicenceDto licence, String repli) {
        String message = licence.getMessage();
        return message != null && !message.isBlank() ? message : repli;
    }

    /** Le plus lointain des jalons, ou le préavis s'ils sont tous illisibles. */
    private long fenetreDAnnonce() {
        long fenetre = preavisJours;
        for (String jalon : jalons.split(",")) {
            String valeur = jalon.trim();
            if (valeur.isEmpty()) {
                continue;
            }
            try {
                fenetre = Math.max(fenetre, Long.parseLong(valeur));
            } catch (NumberFormatException e) {
                log.error("Jalon « {} » illisible dans qualisira.licence.rappels-jalons : il est ignoré.", valeur);
            }
        }
        return fenetre;
    }
}
