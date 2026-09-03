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
@Schema(description = "Critère retenu pour évaluer un fournisseur, et l'appréciation qu'il a "
        + "reçue. Le nom de la classe conserve une coquille d'origine : lire « critère ».")
public class CrictereEvaluationDto extends AuditEntityDto {

    @Schema(description = "Intitulé du critère, tel qu'il apparaît dans la grille d'évaluation.",
            example = "Respect des délais de livraison")
    private String libelleCrictereEvaluation;

    @Schema(description = "Ce que le critère mesure, et ce qui sépare une bonne note d'une "
            + "mauvaise.",
            example = "Écart entre la date promise et la date de livraison effective.")
    private String descriptionCrictereEvaluation;

    @Schema(description = "Note obtenue. Transportée en texte : l'échelle appartient à "
            + "l'organisation, qui peut noter sur cinq comme sur vingt.",
            example = "4/5")
    private String noteAtribuerCritere;

    @Schema(description = "Ce qui a été constaté sur les délais, à l'appui de la note.",
            example = "Deux retards sur douze livraisons.")
    private String delaisLivraison;

    @Schema(description = "Ce qui a été constaté sur la relation et la réactivité du "
            + "fournisseur.",
            example = "Réponse sous quarante-huit heures aux réclamations.")
    private String serviceClient;

    @Schema(description = "Observations de l'évaluateur, hors grille.")
    private String commentaireEvaluation;


   // @JsonBackReference
    @Schema(description = "Identifiant du fournisseur évalué. Seul l'identifiant est transporté : "
            + "porter le fournisseur lui-même ramènerait ses critères, et la réponse se replierait "
            + "sur elle-même.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID fournisseurId;


}
