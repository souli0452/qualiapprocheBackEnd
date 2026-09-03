package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Suivi d'un audit ou d'une inspection menés au titre d'un texte "
        + "réglementaire : ce que le contrôle a établi et ce qu'il reste à faire.")
public class SuiviAuditInspectionDto extends AuditEntityDto {
    @Schema(description = "Ce que le contrôle a établi.",
            example = "Deux écarts mineurs relevés sur l'étiquetage des lots.")
    private String resultatSuiviAuditInspection;

    @Schema(description = "Ce que le contrôleur préconise. Le nom du champ conserve une coquille "
            + "d'origine : lire « action recommandée ».",
            example = "Reprendre l'étiquetage du stock avant la fin du trimestre.")
    private String actionRecommender;

    @Schema(description = "Où en est la mise en œuvre des recommandations. Champ libre : les "
            + "étapes appartiennent à l'organisation.",
            example = "En cours")
    private String statutSuiviAuditInspection;

    @Schema(description = "Textes au titre desquels le contrôle a été mené.")
    private List<ReglementationDto> reglementations;
}
