package com.qualiapproche.amelioration.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 08/03/2022 à 13:12:54
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Demande d'édition d'un état. Elle ne dit pas quoi imprimer mais sur quel "
        + "dossier : les données de l'état sont relues en base à partir de l'identifiant fourni.")
public class ReportingInputDto {

    @Schema(description = "Format de sortie. Absent de la requête, l'état est rendu en PDF.",
            example = "PDF")
    private ReportFormat reportFormat = ReportFormat.PDF;

    @Schema(description = "État à produire, qui désigne à la fois le gabarit et la façon dont le "
            + "dossier est relu. Obligatoire.",
            example = "NON_CONFORMITE")
    @NotNull
    private EReportType reportType;

    @Schema(description = "Dossier sur lequel porte l'état : ici, la non-conformité. Un "
            + "identifiant inconnu est refusé en 400, l'édition ne pouvant rien rendre à vide.")
    private UUID entityId;
}
