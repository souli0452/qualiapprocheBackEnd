package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Les trois nombres des pastilles d'accueil d'un agent. Comptés pour une "
        + "personne et non pour sa structure : deux agents d'un même service n'y lisent pas les "
        + "mêmes chiffres.")
public class NcCountsDto {
    @Schema(description = "Non-conformités que l'agent a déclarées et laissées à l'état de "
            + "brouillon, donc jamais soumises au circuit.",
            example = "2")
    private long brouillons;

    @Schema(description = "Non-conformités qui lui ont été confiées pour traitement. Seul des "
            + "trois nombres à ne pas dépendre de qui a déclaré le dossier, et le seul à ne "
            + "regarder aucun état : un dossier imputé y reste compté après sa clôture.",
            example = "7")
    private long imputees;

    @Schema(description = "Non-conformités qu'il a déclarées et qui sont passées à l'archive.",
            example = "12")
    private long archives;
}
