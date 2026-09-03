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
@Schema(description = "Action type du référentiel, proposée au moment de bâtir un plan "
        + "d'action. Elle nomme un geste ; sa mise en œuvre dans un dossier relève du plan "
        + "d'action.")
public class ActionDto extends AuditEntityDto {

    @Schema(description = "Intitulé de l'action type, tel qu'il apparaît dans les listes de "
            + "choix.",
            example = "Former le personnel concerné")
    private String libelle;

    @Schema(description = "Ce que l'action recouvre, pour que deux structures ne mettent pas le "
            + "même intitulé sur des gestes différents.",
            example = "Session de formation suivie d'une évaluation des acquis.")
    private String description;
}
