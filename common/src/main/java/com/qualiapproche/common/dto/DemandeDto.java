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
@Schema(description = "Une demande adressée au référentiel : son intitulé, son propos et où elle "
        + "en est. Ébauche : aucun point d'entrée ne l'expose encore, et elle ne reprend pas les "
        + "deux dates propres que porte l'entité.")
public class DemandeDto extends AuditEntityDto {
    @Schema(description = "Intitulé court, celui sous lequel la demande se lit dans une liste.",
            example = "Renouvellement du matériel de laboratoire")
    private String libelleDemande;

    @Schema(description = "Exposé de la demande : ce qui est demandé, et pourquoi.")
    private String descriptionDemande;

    @Schema(description = "Où en est la demande. Chaîne libre et non énumération : rien dans le "
            + "modèle n'en borne les valeurs.",
            example = "EN_COURS")
    private String statutDemande;
}
