package com.qualiapproche.support.messagerie;

import com.qualiapproche.common.messagerie.AdressesMessagerie;
import com.qualiapproche.common.messagerie.Dedoublonnage;
import com.qualiapproche.common.messagerie.LecteurDeMessages;
import com.qualiapproche.common.messagerie.RappelDuCircuit;
import com.qualiapproche.common.utils.AuteurDuGeste;
import jakarta.jms.Message;
import com.qualiapproche.support.service.DemandeDocumentService;
import com.qualiapproche.support.service.QmsDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Ce que le moteur de circuit dit des documents et des demandes, après chaque transition.
 *
 * <p>Arrivait par des webhooks HTTP ({@code /internal/callbacks/...}), que workflow-service
 * rejouait lui-même en cas d'échec. Arrive désormais par le courtier : un service momentanément
 * arrêté retrouve ses rappels à son retour, et un rappel relivré est écarté par le dédoublonnage.
 * Une exception rend le message au courtier, qui le relivre.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RappelsDuCircuit {

    private final QmsDocumentService documentService;
    private final DemandeDocumentService demandeService;
    private final Dedoublonnage<MessageTraite> dedoublonnage;
    private final LecteurDeMessages lecteur;

    /**
     * Reçoit un rappel du courtier. Un message illisible est écarté sans nouvelle livraison ; une
     * erreur de traitement le rend au courtier.
     */
    @Transactional
    @JmsListener(destination = AdressesMessagerie.RAPPELS_SUPPORT)
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
            case "DOCUMENT" -> {
                // `status` pilote le cycle de vie du document ; `statusName` n'est que le libellé de
                // l'étape courante.
                documentService.updateWorkflowStatus(rappel.getRessourceId(), (String) contenu.get("status"),
                        (String) contenu.get("statusName"), (String) contenu.get("comments"));
                Object commentaires = contenu.get("comments");
                documentService.logWorkflowAudit(rappel.getRessourceId(), "TRANSITION_" + contenu.get("decision"),
                        commentaires != null ? commentaires.toString()
                                : "Passage à l'étape « " + contenu.get("statusName") + " »");
            }
            // L'aboutissement d'une demande ne se réduit pas à un état : accepter une modification
            // ouvre le dépôt du remplaçant, accepter une suppression retire le document.
            case "DEMANDE_DOCUMENT" -> demandeService.traiterAvancement(rappel.getRessourceId(), contenu);
            default -> log.warn("Rappel ignoré : le type {} ne relève pas de support-service.", rappel.getTypeRessource());
        }
    }
}
