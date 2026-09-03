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

@Schema(description = "Produit ou service que l'organisation réalise, tenu au référentiel. C'est "
        + "l'objet sur lequel portent la conformité et la satisfaction du client, et à ce titre "
        + "il se nomme une fois pour toutes plutôt que d'être ressaisi dossier après dossier.")
public class ProduitDto extends AuditEntityDto {
    @Schema(description = "Désignation du produit, telle qu'elle apparaît dans les listes de "
            + "choix.",
            example = "Analyse biologique de routine")
    private String libelleProduit;

    @Schema(description = "Ce que le produit recouvre : ce qui fait qu'une prestation en relève "
            + "plutôt que de la voisine.",
            example = "Prélèvement, analyse et rendu des examens courants de biologie médicale.")
    private String descriptionProduit;

    @Schema(description = "Entités auditées auxquelles ce produit se rattache. Le référentiel ne "
            + "tient pas ce lien : la liste revient vide, et ce qui y est envoyé n'est pas "
            + "conservé.")
    private List<AuditeDto> audites;

}
