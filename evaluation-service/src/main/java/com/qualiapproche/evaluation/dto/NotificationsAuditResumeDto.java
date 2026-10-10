package com.qualiapproche.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ce que l'utilisateur connecté a en attente dans le module Audit, en nombres : un compteur par
 * onglet du module, et leur somme pour la pastille du menu. Pendant chiffré des lignes de la
 * cloche, adossé aux mêmes sources.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsAuditResumeDto {

    @Schema(description = "La somme de tout ce qui attend l'appelant : la pastille du menu")
    private long total;

    @Schema(description = "Onglet Programme : audits à valider et audits en retard")
    private long programme;

    @Schema(description = "Onglet Constatations : points à saisir, brouillons à publier, constats compilés à valider")
    private long constats;

    @Schema(description = "Onglet Suivi : écarts RQAP-BF publiés dont la transmission au module NC attend")
    private long suivi;

    @Schema(description = "Onglet Checklists : modèles encore en brouillon")
    private long checklists;

    @Schema(description = "Signatures de rapport qui attendent l'appelant")
    private long signatures;
}
