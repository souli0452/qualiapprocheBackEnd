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
@Schema(description = "Prestataire de services référencé par l'organisation. Distinct du "
        + "fournisseur, qui livre des biens et se voit évalué sur une grille de critères : le "
        + "prestataire n'en porte pas.")
public class PrestataireDto extends AuditEntityDto {
    @Schema(description = "Raison sociale, telle qu'elle figure sur les pièces contractuelles.",
            example = "Sahel Maintenance SARL")
    private String nomPrestataire;

    @Schema(description = "Adresse du siège ou de l'établissement avec lequel on traite.",
            example = "01 BP 1234, Ouagadougou 01")
    private String adressePrestataire;

    @Schema(description = "Numéro auquel joindre le prestataire.", example = "+226 25 30 12 34")
    private String telephonePrestataire;

    @Schema(description = "Personne à qui s'adresser chez le prestataire, plutôt que l'accueil.",
            example = "Issa Kaboré, responsable des contrats")
    private String contactPrincipalPrestataire;

    @Schema(description = "Adresse à laquelle écrire au prestataire.",
            example = "contact@sahel-maintenance.bf")
    private String emailPrestataire;

    @Schema(description = "Site du prestataire, s'il en a un.",
            example = "https://www.sahel-maintenance.bf")
    private String siteWebPrestataire;

    @Schema(description = "Où en est la relation : référencé, suspendu, écarté. Champ libre, "
            + "l'organisation nomme elle-même les situations qu'elle distingue.",
            example = "Actif")
    private String statutPrestataire;
}
