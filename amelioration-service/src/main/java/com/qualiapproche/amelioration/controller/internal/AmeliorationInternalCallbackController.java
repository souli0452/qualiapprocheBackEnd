package com.qualiapproche.amelioration.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.amelioration.service.PlanActionService;
import org.springframework.security.access.prepost.PreAuthorize;

/*
 * Réservé aux services ({@code @perm.appelDeService()}) : ces points de rappel écrivent l'état
 * métier sans passer par une décision de circuit. Sous la seule authentification, n'importe quel
 * agent muni d'un jeton valide pouvait y poster un statut « validé ».
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/callbacks")
@PreAuthorize("@perm.appelDeService()")
@RequiredArgsConstructor
public class AmeliorationInternalCallbackController {

    private final NonConformiteService nonConformiteService;
    private final PlanActionService planActionService;

    @PostMapping("/non-conformites/{ncId}/status")
    public ResponseEntity<Void> updateNonConformiteStatus(
            @PathVariable("ncId") UUID ncId,
            @RequestBody Map<String, Object> payload) {

        log.info("Received workflow callback for NC {}: {}", ncId, payload);

        // Le moteur établit lui-même l'issue de la transition — EN_COURS, APPROVED, REJECTED — et
        // l'état de traitement de l'étape atteinte. Seul le libellé était transmis jusqu'ici, à
        // charge pour le service de retrouver l'issue en comparant des noms d'étapes : renommer
        // une étape dans l'éditeur de circuits suffisait à faire retomber la NC en « en cours ».
        nonConformiteService.updateWorkflowState(ncId,
                (String) payload.get("status"),
                (String) payload.get("statusName"),
                (String) payload.get("etatCode"),
                champsSaisis(payload));

        return ResponseEntity.ok().build();
    }

    /**
     * Valeurs saisies à l'étape, telles que le moteur les a enregistrées.
     *
     * <p>Elles restaient dans l'historique du moteur : un document justificatif déposé au moment
     * du rejet n'atteignait jamais la non-conformité, qu'il fallait donc lui redemander par un
     * autre écran. La conversion est défensive — la charge utile vient du réseau, rien ne garantit
     * la forme attendue.</p>
     */
    private Map<String, String> champsSaisis(Map<String, Object> payload) {
        Object champs = payload.get("fields");
        if (!(champs instanceof Map<?, ?> saisies)) {
            return Map.of();
        }
        Map<String, String> resultat = new HashMap<>();
        saisies.forEach((nom, valeur) -> {
            if (nom != null && valeur != null) {
                resultat.put(nom.toString(), valeur.toString());
            }
        });

        if (payload.containsKey("comments") && payload.get("comments") != null) {
            resultat.put("WORKFLOW_COMMENTS", payload.get("comments").toString());
        }
        return resultat;
    }

    @PostMapping("/plan-actions/{paId}/status")
    public ResponseEntity<Void> updatePlanActionStatus(
            @PathVariable("paId") UUID paId,
            @RequestBody Map<String, Object> payload) {

        log.info("Received workflow callback for PlanAction {}: {}", paId, payload);

        // Les saisies valent pour le plan comme pour la non-conformité : c'est par elles que la
        // ré-attribution nomme le nouveau responsable.
        planActionService.updateWorkflowState(paId,
                (String) payload.get("statusName"),
                (String) payload.get("etatCode"),
                champsSaisis(payload));

        return ResponseEntity.ok().build();
    }
}
