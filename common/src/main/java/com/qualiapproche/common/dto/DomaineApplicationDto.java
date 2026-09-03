package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domaine d'application d'un document — le champ d'activité qu'il couvre.
 *
 * <p>C'était une saisie libre : chaque rédacteur écrivait « RH », « Ressources Humaines » ou
 * « ressources humaines », et le regroupement statistique par domaine comptait trois domaines là
 * où il n'y en avait qu'un.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Domaine d'application d'un document : le champ d'activité qu'il couvre. "
        + "Huit domaines usuels sont semés au premier démarrage, que l'organisation modifie et "
        + "complète ensuite. Le choix passe par cette liste plutôt que par une saisie libre, où "
        + "« RH » et « Ressources humaines » comptaient pour deux domaines dans les statistiques.")
public class DomaineApplicationDto extends AuditEntityDto {
    @Schema(description = "Intitulé du domaine, tel qu'il apparaît dans les listes de choix. Il "
            + "est unique : deux domaines ne peuvent porter le même nom, faute de quoi le "
            + "regroupement statistique perdrait son sens.",
            example = "Sécurité de l'information")
    private String libelle;

    @Schema(description = "Ce que le domaine recouvre : ce qui fait qu'un document relève de lui "
            + "plutôt que du voisin.",
            example = "Management de la sécurité de l'information (SMSI)")
    private String description;

    @Schema(description = "Rang d'affichage dans les listes de choix. Il place les domaines les "
            + "plus courants en tête, là où l'ordre alphabétique les disperserait. Facultatif : "
            + "un domaine sans rang se range après ceux qui en ont un.",
            example = "4")
    private Integer ordre;
}
