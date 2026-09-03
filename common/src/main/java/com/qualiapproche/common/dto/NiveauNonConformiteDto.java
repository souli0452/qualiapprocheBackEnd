package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Gravité d'une non-conformité, paramétrable par l'organisation.
 *
 * <p>Volontairement ouvert : « Mineure », « Majeure », « Critique » — c'est à la démarche qualité
 * de dire ce qu'elle distingue, non au logiciel.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Gravité d'une non-conformité, paramétrée par l'organisation. Les niveaux "
        + "ne sont pas imposés : c'est à la démarche qualité de dire ce qu'elle distingue.")
public class NiveauNonConformiteDto extends AuditEntityDto {

    @Schema(description = "Intitulé du niveau, tel qu'il apparaît dans les listes de choix.",
            example = "Majeure")
    private String libelle;

    @Schema(description = "Ce que ce niveau signifie pour vos équipes : les critères qui font "
            + "qu'un constat relève de lui plutôt que du voisin.",
            example = "Écart susceptible d'altérer la qualité du livrable ou de rompre une "
                    + "exigence contractuelle.")
    private String description;

    /**
     * Poids du niveau, du moins grave au plus grave. Deux niveaux de même score restent acceptés :
     * l'ordre entre eux n'est alors pas garanti, ce qui ne prête pas à conséquence.
     */
    @Schema(description = "Poids du niveau, du moins grave au plus grave. Il sert au tri et à la "
            + "comparaison, que le libellé ne permet pas : « Critique » précède « Majeure » dans "
            + "l'ordre alphabétique, à l'inverse de la gravité. L'étendue de l'échelle appartient "
            + "à l'organisation. Facultatif : un niveau sans score se range après ceux qui en ont "
            + "un.",
            example = "2")
    private Integer score;

    /** Couleur d'affichage du niveau (ex. {@code #f59e0b}), facultative. */
    @Schema(description = "Couleur d'affichage du niveau, au format hexadécimal. Facultative : à "
            + "défaut, l'écran applique sa propre teinte.",
            example = "#f59e0b")
    private String couleur;
}
