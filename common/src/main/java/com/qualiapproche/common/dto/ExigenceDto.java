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
@Schema(description = "Exigence à satisfaire, tirée d'un texte réglementaire, d'une norme ou "
        + "d'un contrat, avec son échéance et l'état de sa conformité.")
public class ExigenceDto extends AuditEntityDto {

    @Schema(description = "Intitulé de l'exigence, tel qu'il apparaît dans les listes de suivi.",
            example = "Étalonnage annuel des instruments de pesée")
    private String libelleExigence;

    @Schema(description = "Ce que l'exigence impose, et la disposition du texte dont elle est "
            + "tirée.",
            example = "Vérification par un organisme accrédité, une fois l'an.")
    private String descriptionExigence;

    @Schema(description = "Date à laquelle l'exigence doit être satisfaite. Transportée en texte : "
            + "le serveur n'en impose pas le format.",
            example = "31-12-2026")
    private String dateEcheanceExigence;

    @Schema(description = "État de la conformité à cette exigence. Champ libre : l'échelle "
            + "appartient à l'organisation.",
            example = "Conforme")
    private String statutConformite;

    @Schema(description = "Entités contrôlées au regard de cette exigence.")
    private List<AuditeDto> audites;

    @Schema(description = "Actions engagées pour atteindre ou rétablir la conformité.")
    private List<ActionCorrectivePreventiveDto> actionCorrectivePreventives;
}
