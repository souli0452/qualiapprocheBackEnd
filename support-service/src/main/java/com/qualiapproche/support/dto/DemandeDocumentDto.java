package com.qualiapproche.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Demande de modification ou de suppression, telle que la présentent les écrans. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeDocumentDto {
    private UUID id;
    private UUID documentId;
    private String documentNumber;
    private String documentTitre;
    private String type;
    private String etat;
    private String objectif;
    private String description;
    private String structureId;
    private String structureLibelle;
    private String demandeurId;
    private String demandeurNom;
    private String pieceJointeNom;
    private UUID workflowId;
    private String currentEtape;
    private LocalDateTime createdAt;
    private LocalDateTime dateDecision;
    private String motifDecision;
    private LocalDateTime dateExecution;

    /**
     * Vrai lorsque la demande est acceptée et attend son exécution : dépôt du fichier remplaçant
     * pour une modification, confirmation du retrait pour une suppression.
     */
    private boolean enAttenteExecution;

    /**
     * État du circuit : étape courante, décisions ouvertes à l'appelant, champs à renseigner.
     *
     * <p>Renseigné par les listes qui proposent d'agir — la vue d'ensemble — et nul ailleurs. Une
     * liste qui annonce des décisions à prendre sans porter les actions du moteur oblige à ouvrir
     * chaque fiche pour découvrir ce qu'on peut y faire ; et les demander une par une multipliait
     * les allers-retours autant que de lignes.</p>
     */
    private com.qualiapproche.common.dto.WorkflowStateDto workflowState;
}
