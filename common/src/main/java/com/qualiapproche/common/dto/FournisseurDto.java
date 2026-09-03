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
@Schema(description = "Fournisseur référencé par l'organisation, avec la grille sur laquelle il "
        + "est évalué. L'évaluation périodique des fournisseurs est une exigence de la démarche "
        + "qualité : c'est ce qui distingue cette fiche d'un simple carnet d'adresses.")
public class FournisseurDto extends AuditEntityDto {
    @Schema(description = "Raison sociale, telle qu'elle figure sur les pièces contractuelles.",
            example = "Faso Bureautique SA")
    private String nom;

    @Schema(description = "Adresse du siège ou de l'établissement avec lequel on traite.",
            example = "05 BP 678, Bobo-Dioulasso 05")
    private String adresse;

    @Schema(description = "Numéro auquel joindre le fournisseur.", example = "+226 20 97 45 678")
    private String telephone;

    @Schema(description = "Adresse à laquelle écrire au fournisseur.",
            example = "commandes@faso-bureautique.bf")
    private String email;

    @Schema(description = "Site du fournisseur, s'il en a un.",
            example = "https://www.faso-bureautique.bf")
    private String siteWeb;

    @Schema(description = "Personne à qui s'adresser chez le fournisseur, plutôt que l'accueil.",
            example = "Mariam Sanou, chargée de clientèle")
    private String contactPrincipal;

    @Schema(description = "Où en est la relation : référencé, suspendu, écarté. Champ libre, "
            + "l'organisation nomme elle-même les situations qu'elle distingue.",
            example = "Actif")
    private String statut;

    @Schema(description = "Critères sur lesquels ce fournisseur a été apprécié, et les notes "
            + "obtenues. Une fiche sans critère n'est pas une fiche mal remplie : elle dit que le "
            + "fournisseur n'a pas encore été évalué.")
    private List<CrictereEvaluationDto> criteresEvaluation;

}
