package com.qualiapproche.common.messagerie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Ce que le moteur de circuit dit au service propriétaire d'un dossier, après une transition :
 * l'issue, l'étape atteinte, les champs saisis. Remplace les webhooks HTTP
 * ({@code /internal/callbacks/...}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RappelDuCircuit {
    /** DOCUMENT, DEMANDE_DOCUMENT, NON_CONFORMITE, PLAN_ACTION. */
    private String typeRessource;
    private UUID ressourceId;
    /** status, statusName, etatCode, fields, conditionFranchie, decision, comments… */
    private Map<String, Object> contenu;
    /**
     * Qui a fait franchir la transition, et son nom à ce moment-là : le service destinataire
     * attribue sa mise à jour à cette personne ({@link com.qualiapproche.common.utils.AuteurDuGeste}).
     * Vides pour un geste sans auteur (replacement automatique, tâche planifiée).
     */
    private String auteurId;
    private String auteurNom;
}
