package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Niveau de confidentialité d'un document, et rôles admis à le consulter.
 *
 * <p>La restriction s'ajoute à celle de la structure, elle ne s'y substitue pas : pour voir un
 * document, il faut relever de sa structure (ou en avoir reçu le partage) <b>et</b> détenir l'un
 * des rôles listés ici. Un niveau sans rôle ne restreint rien — c'est le niveau ordinaire.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Niveau de confidentialité d'un document et rôles admis à le consulter. La "
        + "restriction s'ajoute à celle de la structure, elle ne s'y substitue pas : il faut "
        + "relever de la structure du document, ou en avoir reçu le partage, et détenir l'un des "
        + "rôles listés.")
public class NiveauConfidentialiteDto extends AuditEntityDto {
    @Schema(description = "Intitulé du niveau, tel qu'il apparaît dans les listes de choix. Il "
            + "est unique.",
            example = "Diffusion restreinte")
    private String libelle;

    @Schema(description = "Ce que ce niveau protège, et pourquoi : les critères qui font qu'un "
            + "document en relève plutôt que du voisin.",
            example = "Documents dont la diffusion hors de la direction concernée doit rester "
                    + "exceptionnelle.")
    private String description;

    /** Rang, du moins sensible au plus sensible. */
    @Schema(description = "Rang, du moins sensible au plus sensible. Il sert au tri et à la "
            + "comparaison, que le libellé ne permet pas. Facultatif : un niveau sans rang se "
            + "range après ceux qui en ont un, départagé par son libellé.",
            example = "3")
    private Integer ordre;

    /**
     * Rôles admis à consulter un document de ce niveau, désignés par leur nom
     * (« RESPONSABLE_QUALITE », « PILOTE »…). Liste vide : aucune restriction de rôle.
     */
    @Schema(description = "Rôles admis à consulter un document de ce niveau, désignés par leur "
            + "nom et non par leur identifiant : le nom est immuable une fois le rôle publié, et "
            + "c'est déjà lui que portent les étapes des circuits. Liste vide, le niveau ne "
            + "restreint rien de plus que la règle de structure.",
            example = "[\"RESPONSABLE_QUALITE\", \"PILOTE\"]")
    @lombok.Builder.Default
    private List<String> rolesAutorises = new ArrayList<>();
}
