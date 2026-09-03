package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Contrat ou accord conclu avec un prestataire : ce qui a été convenu, et "
        + "depuis quand. Le référentiel en tient la trace pour que les dossiers puissent s'y "
        + "référer ; aucun point d'entrée ne l'expose encore.")
public class ContratAccordDto extends AuditEntityDto {
    @Schema(description = "Intitulé sous lequel l'accord est cité, plus court que son objet.",
            example = "Maintenance du parc informatique 2026")
    private String libelleContratAccord;

    @Schema(description = "Objet de l'accord : ce qu'il engage de part et d'autre.",
            example = "Maintenance préventive et curative des postes et des serveurs, "
                    + "intervention sous quarante-huit heures.")
    private String descriptionContratAccord;

    @Schema(description = "Date de signature. C'est elle qui fait foi pour l'ancienneté de la "
            + "relation, et non la date de création de la fiche, souvent bien postérieure.")
    private java.time.LocalDateTime dateSignature;

    @Schema(description = "Prestataire engagé par cet accord. Seul l'identifiant est transporté : "
            + "sa fiche se demande à part.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID prestataireId;
}
