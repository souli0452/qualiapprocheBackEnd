package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Un critère sur lequel l'équipe évalue un auditeur à l'issue d'un audit. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Critère d'évaluation des auditeurs, paramétré par l'organisation.")
public class CritereEvaluationAuditeurDto extends AuditEntityDto {

    @Schema(description = "Intitulé du critère.", example = "Maîtrise du référentiel")
    private String libelle;

    @Schema(description = "Faux une fois le critère retiré : il ne se propose plus dans les choix.")
    private boolean actif;
}
