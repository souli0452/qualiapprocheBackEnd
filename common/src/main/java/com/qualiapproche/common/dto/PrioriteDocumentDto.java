package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Niveau de priorité d'un document, paramétrable par l'organisation.
 *
 * <p>Volontairement ouvert : « Urgent », « Normal », « À traiter avant le 15 » — c'est à la
 * démarche qualité de dire ce qu'elle distingue, non au logiciel.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Niveau de priorité d'un document, paramétré par l'organisation. Les "
        + "niveaux ne sont pas imposés : c'est à la démarche qualité de dire ce qu'elle distingue.")
public class PrioriteDocumentDto extends AuditEntityDto {

    @Schema(description = "Intitulé de la priorité, tel qu'il apparaît dans les listes de choix.",
            example = "Urgent")
    private String libelle;

    @Schema(description = "Ce que ce niveau signifie pour vos équipes : ce qui justifie qu'un "
            + "document soit traité avant un autre.",
            example = "Document bloquant une mise en service ou attendu par un audit.")
    private String description;

    /**
     * Poids de la priorité, du plus urgent au moins urgent. Deux priorités de même score restent
     * acceptées : l'ordre entre elles n'est alors pas garanti, ce qui ne prête pas à conséquence.
     */
    @Schema(description = "Poids de la priorité, du plus urgent au moins urgent. Il sert au tri et "
            + "à la comparaison, que le libellé ne permet pas : « Normal » précède « Urgent » dans "
            + "l'ordre alphabétique, à l'inverse de l'urgence. L'étendue de l'échelle appartient à "
            + "l'organisation. Facultatif : une priorité sans score se range après celles qui en "
            + "ont un.",
            example = "1")
    private Integer score;

    /** Couleur d'affichage (ex. {@code #dc2626}), facultative. */
    @Schema(description = "Couleur d'affichage de la priorité, au format hexadécimal. "
            + "Facultative : à défaut, l'écran applique sa propre teinte.",
            example = "#dc2626")
    private String couleur;
}
