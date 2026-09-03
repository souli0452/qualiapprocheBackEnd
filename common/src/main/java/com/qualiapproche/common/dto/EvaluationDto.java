package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Modalité d'évaluation du référentiel : ce sur quoi porte l'appréciation "
        + "et comment elle est recueillie. Elle nomme la modalité, non son résultat.")
public class EvaluationDto extends AuditEntityDto {
    @Schema(description = "Intitulé de la modalité, tel qu'il apparaît dans les listes de choix.",
            example = "Évaluation à chaud")
    private String libelle;

    @Schema(description = "Ce que l'évaluation cherche à établir, et de quelle manière.",
            example = "Questionnaire remis en fin de session, portant sur l'atteinte des objectifs.")
    private String description;
}
