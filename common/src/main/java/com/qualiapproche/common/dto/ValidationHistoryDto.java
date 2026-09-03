package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrée de traçabilité d'un circuit, telle que les modules métier la reçoivent du moteur.
 *
 * <p>Pendant de {@code com.qualiapproche.workflow.dto.ValidationHistoryDto} : comme pour l'état de
 * circuit, une propriété publiée par le moteur mais absente ici serait perdue en silence à la
 * désérialisation. {@code ContratDesChampsPartagesTest} (workflow-service) tient les deux classes
 * alignées.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Une décision prise sur un dossier, telle qu'elle a été consignée : qui, "
        + "quand, à quelle étape, et avec quelles valeurs saisies. Pièce d'audit, jamais réécrite — "
        + "elle dit ce qui était vrai à sa date, non ce qui l'est aujourd'hui.")
public class ValidationHistoryDto {
    @Schema(description = "Identifiant de la ligne d'historique. Sert à la désigner, non à la "
            + "situer : c'est la date de décision qui donne l'ordre du parcours.",
            example = "418")
    private Long id;

    @Schema(description = "Étape que le dossier quittait, désignée par son identifiant technique. "
            + "Il vaut pour cette installation seule, et deux lignes portant le même désignent bien "
            + "la même étape traversée deux fois.",
            example = "42")
    private String stepCode;

    @Schema(description = "Nom que portait cette étape, conservé avec la décision : un circuit "
            + "remanié ne rend pas illisible l'historique des dossiers déjà passés.",
            example = "Vérification")
    private String stepName;

    @Schema(description = "Ce qui a été fait, sous le libellé du bouton actionné — et non sous le "
            + "nom de la nature de la décision. Le texte suit donc la configuration du circuit et "
            + "n'appartient à aucun ensemble fermé ; il n'y a rien à y brancher.",
            example = "Retourner au rédacteur")
    private String decision;

    @Schema(description = "Ce que le décideur a écrit en se prononçant : le motif d'un renvoi, la "
            + "réserve d'une approbation.",
            example = "Le paragraphe 3 renvoie à une procédure abrogée.")
    private String comments;

    @Schema(description = "Identifiant de qui a décidé. Le seul repère sur les décisions "
            + "antérieures à la conservation du nom.",
            example = "8c1f9b74-2d3e-4a55-9f0c-71b2ad6e5c48")
    private String validatorUserId;
    /** Nom de l'auteur au moment de la décision ; nul sur les décisions antérieures à ce champ. */
    @Schema(description = "Nom du décideur figé au moment où il s'est prononcé : un agent qui "
            + "change de nom ou quitte l'organisation ne réécrit pas l'histoire des dossiers. Vide "
            + "sur les décisions antérieures à ce champ.",
            example = "Awa Traoré")
    private String validatorFullName;

    @Schema(description = "Quand la décision a été prise. C'est elle qui ordonne le parcours du "
            + "dossier, rendu du plus ancien au plus récent.",
            example = "2026-03-14T09:25:00")
    private LocalDateTime decisionDate;

    @Schema(description = "Ce qui a été saisi à cette décision précise. Une correction ultérieure "
            + "du même champ ajoute une valeur à une autre décision sans effacer celle-ci : "
            + "l'historique garde les deux, là où l'état du circuit ne montre que la dernière.")
    @Builder.Default
    private List<FieldValueDto> fieldValues = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Une valeur saisie au moment de cette décision, conservée avec de quoi "
            + "rester lisible sans consulter le circuit.")
    public static class FieldValueDto {
        @Schema(description = "Identifiant du champ d'étape qui a recueilli la valeur. Il peut "
                + "désigner un champ depuis retiré du circuit, d'où la conservation de l'intitulé "
                + "à côté.",
                example = "58")
        private String fieldCode;

        @Schema(description = "Nom technique du champ, seul repère qu'un module métier puisse "
                + "reconnaître pour retrouver une donnée précise.",
                example = "observationsVerification")
        private String fieldName;
        /** Intitulé du champ tel qu'il était présenté à qui a saisi ; à défaut, le nom technique. */
        @Schema(description = "Intitulé sous lequel la donnée a été demandée, recopié plutôt que "
                + "relu sur le circuit : une saisie doit rester lisible plus longtemps que le "
                + "formulaire qui l'a recueillie. À défaut, le nom technique.",
                example = "Observations du vérificateur")
        private String fieldLabel;

        @Schema(description = "La valeur, sous la forme où elle a été saisie. Toujours du texte, "
                + "quel que soit le type du champ ; une liste alimentée par une source y laisse "
                + "l'identifiant retenu, non son libellé.",
                example = "Conforme après reprise")
        private String value;
    }
}
