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
@Schema(description = "Texte opposable à l'organisation — loi, décret, arrêté ou norme — et les "
        + "exigences qui en sont tirées.")
public class ReglementationDto extends AuditEntityDto {

    @Schema(description = "Intitulé du texte, tel qu'il est cité dans les documents de "
            + "l'organisation.",
            example = "Norme ISO 9001:2015")
    private String nomReglementation;

    @Schema(description = "Ce que le texte impose, dans les termes de l'organisation plutôt que "
            + "dans les siens.",
            example = "Exige un système de management de la qualité documenté et audité.")
    private String descriptionReglementation;

    @Schema(description = "Autorité dont le texte émane et qui en contrôle l'application.",
            example = "ABNORM")
    private String organismeReglementation;

    @Schema(description = "Exigences tirées du texte, telles que l'organisation les a découpées "
            + "pour pouvoir les suivre une à une.")
    private List<ExigenceDto> exigences;

    @Schema(description = "Contrôles menés au titre de ce texte.")
    private List<SuiviAuditInspectionDto> suiviAuditInspections;
}
