package com.qualiapproche.amelioration.messagerie;

import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.amelioration.service.PlanActionService;
import com.qualiapproche.common.messagerie.AdressesMessagerie;
import com.qualiapproche.common.messagerie.Dedoublonnage;
import com.qualiapproche.common.messagerie.LecteurDeMessages;
import com.qualiapproche.common.messagerie.RappelDuCircuit;
import com.qualiapproche.common.utils.AuteurDuGeste;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Ce que le moteur de circuit dit des non-conformités et des plans d'action, après chaque
 * transition.
 *
 * <p>Arrivait par des webhooks HTTP ({@code /internal/callbacks/...}). Arrive désormais par le
 * courtier : un service momentanément arrêté retrouve ses rappels à son retour, et un rappel
 * relivré est écarté par le dédoublonnage. Une exception rend le message au courtier.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RappelsDuCircuit {

    private final NonConformiteService nonConformiteService;
    private final PlanActionService planActionService;
    private final Dedoublonnage<MessageTraite> dedoublonnage;
    private final LecteurDeMessages lecteur;

    /**
     * Reçoit un rappel du courtier. Un message illisible est écarté sans nouvelle livraison ; une
     * erreur de traitement le rend au courtier.
     */
    @Transactional
    @JmsListener(destination = AdressesMessagerie.RAPPELS_AMELIORATION)
    public void surMessage(Message message) {
        lecteur.lire(message, RappelDuCircuit.class).ifPresent(rappel -> recevoir(rappel, lecteur.identifiant(message)));
    }

    /**
     * Applique un rappel, au nom de la personne qui a fait franchir la transition : le dossier
     * mis à jour porte son nom, comme si elle l'avait modifié elle-même.
     */
    public void recevoir(RappelDuCircuit rappel, String identifiant) {
        if (rappel.getTypeRessource() == null || rappel.getRessourceId() == null) {
            // Ne guérira pas : relivré, il reviendrait trois jours durant.
            log.error("Rappel {} écarté : type de ressource ou identifiant absent.", identifiant);
            return;
        }
        if (!dedoublonnage.premiereLivraison(identifiant)) {
            return;
        }
        Map<String, Object> contenu = rappel.getContenu() == null ? Map.of() : rappel.getContenu();
        // Sans le contenu : il porte les champs saisis et les commentaires, qui n'ont rien à faire
        // dans un journal.
        log.info("Rappel du circuit pour {} {} (étape {}).", rappel.getTypeRessource(), rappel.getRessourceId(),
                contenu.get("etatCode"));
        AuteurDuGeste.pour(rappel.getAuteurId(), rappel.getAuteurNom(), () -> appliquer(rappel, contenu));
    }

    private void appliquer(RappelDuCircuit rappel, Map<String, Object> contenu) {
        switch (rappel.getTypeRessource().toUpperCase()) {
            // Le moteur établit lui-même l'issue de la transition et l'état de traitement de l'étape
            // atteinte ; le point de contrôle franchi dit ce que la décision vient de vérifier.
            case "NON_CONFORMITE" -> nonConformiteService.updateWorkflowState(rappel.getRessourceId(),
                    (String) contenu.get("status"), (String) contenu.get("statusName"),
                    (String) contenu.get("etatCode"), champsSaisis(contenu), (String) contenu.get("conditionFranchie"));
            // Les saisies valent pour le plan comme pour la non-conformité : c'est par elles que la
            // ré-attribution nomme le nouveau responsable.
            case "PLAN_ACTION" -> planActionService.updateWorkflowState(rappel.getRessourceId(),
                    (String) contenu.get("statusName"), (String) contenu.get("etatCode"), champsSaisis(contenu));
            default -> log.warn("Rappel ignoré : le type {} ne relève pas d'amelioration-service.",
                    rappel.getTypeRessource());
        }
    }

    private static Map<String, String> champsSaisis(Map<String, Object> contenu) {
        if (!(contenu.get("fields") instanceof Map<?, ?> saisies)) {
            return Map.of();
        }
        Map<String, String> resultat = new HashMap<>();
        saisies.forEach((nom, valeur) -> {
            if (nom != null && valeur != null) {
                resultat.put(nom.toString(), valeur.toString());
            }
        });
        return resultat;
    }
}
