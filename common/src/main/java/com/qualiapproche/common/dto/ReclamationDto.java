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
@Schema(description = "Réclamation adressée par un client ou un usager. Elle est l'un des faits "
        + "qui peuvent donner naissance à une non-conformité.")
public class ReclamationDto extends AuditEntityDto {
    @Schema(description = "Référence sous laquelle la réclamation est enregistrée puis citée dans "
            + "les échanges.",
            example = "REC-2026-017")
    private String numeroReference;

    @Schema(description = "Nom de la personne ou de l'organisation qui réclame. Le nom du champ "
            + "conserve une coquille d'origine : lire « demandeur ».",
            example = "Coopérative agricole de Banfora")
    private String nomDemendeur;
}
