package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Un niveau de l'échelle d'évaluation des auditeurs.
 *
 * <p>{@code ordre} en fait aussi la valeur : c'est sur lui que se calcule la moyenne d'un
 * auditeur. Le module Audit le recopie au moment d'évaluer, si bien que retoucher l'échelle ne
 * réécrit pas les évaluations passées.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Niveau de l'échelle d'évaluation des auditeurs, paramétré par l'organisation.")
public class NiveauEvaluationAuditeurDto extends AuditEntityDto {

    @Schema(description = "Intitulé du niveau.", example = "Satisfaisant")
    private String libelle;

    @Schema(description = "Rang et valeur du niveau ; le plus haut est le meilleur.", example = "3")
    private int ordre;

    @Schema(description = "Faux une fois le niveau retiré : il ne se propose plus dans les choix.")
    private boolean actif;
}
