package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gabarit d'un courriel envoyé par le moteur de circuits. Les gabarits sont "
        + "livrés avec le produit et modifiables ensuite ; un gabarit devenu illisible n'empêche "
        + "pas l'envoi, le corps part alors sans substitution.")
public class EmailTemplateDto {
    @Schema(description = "Identifiant du gabarit. Absent à la création, où le serveur "
            + "l'attribue.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID id;

    @Schema(description = "Nom par lequel le code réclame ce gabarit au moment d'écrire. Unique, "
            + "et à ne pas retoucher : sous un autre nom, l'étape correspondante n'enverrait plus "
            + "rien.",
            example = "validationRq")
    private String code;

    @Schema(description = "Objet du message. Il admet les mêmes variables que le corps.",
            example = "Validation attendue - Non-Conformité {numeroNc}")
    private String subject;

    @Schema(description = "Corps du message, en HTML. Les variables s'y écrivent entre accolades — "
            + "{numeroNc} — et seules celles que l'envoi fournit sont remplacées, ce qui laisse "
            + "intactes les accolades de la feuille de style. Une retouche est perdue si "
            + "l'installation est réglée pour rétablir les gabarits livrés au démarrage.")
    private String body;

    @Schema(description = "À quelle occasion ce gabarit part, pour que l'écran d'administration "
            + "dise autre chose qu'un code.",
            example = "Validation attendue par le responsable qualité")
    private String description;
}
