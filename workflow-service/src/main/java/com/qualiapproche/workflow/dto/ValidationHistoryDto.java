package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrée de traçabilité d'un circuit de validation : qui a décidé quoi, quand, à quelle étape,
 * et avec quelles valeurs saisies.
 *
 * <p>Ces enregistrements existaient en base mais n'étaient exposés par aucun point d'entrée :
 * les valeurs de champs saisies à chaque décision étaient écrites puis jamais relues, alors que
 * la restitution de l'historique est l'attendu d'audit de la démarche qualité.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Une décision prise au cours d'un circuit, avec son auteur, sa date et ce "
        + "qui a été saisi à cette occasion. Enregistrement d'audit : il dit ce qui était vrai à "
        + "sa date, et rien n'y est mis à jour par la suite.")
public class ValidationHistoryDto {

    @Schema(description = "Identifiant de l'entrée. Les entrées sont rendues dans l'ordre "
            + "chronologique de la décision, non dans celui de cet identifiant.")
    private Long id;

    @Schema(description = "Étape que le dossier quittait, et non celle où la décision l'a conduit.",
            example = "VALIDATION_PILOTE")
    private String stepCode;

    @Schema(description = "Nom de cette étape de départ, tel qu'il était alors.",
            example = "Validation par le pilote")
    private String stepName;

    @Schema(description = "Libellé du bouton d'action qui a été actionné — « Approuver et mettre "
            + "en vigueur », « Renvoyer au déclarant ». Ce n'est pas la nature de la décision : "
            + "APPROUVE et REJETE ne figurent pas ici, et une transition sans libellé est "
            + "consignée « Action exécutée ».",
            example = "Transmettre pour approbation")
    private String decision;

    @Schema(description = "Commentaire porté par l'auteur au moment de décider. C'est de lui que "
            + "les modules métier tirent le motif d'un refus.")
    private String comments;

    @Schema(description = "Identifiant Keycloak de qui a décidé. Il reste le seul repère certain "
            + "si la personne quitte l'organisation.")
    private String validatorUserId;

    /** Nom de l'auteur au moment de la décision ; nul sur les décisions antérieures à ce champ. */
    @Schema(description = "Nom de l'auteur tel qu'il se présentait à la date de la décision, figé "
            + "depuis : le résoudre à la lecture ferait mentir la trace. Nul sur les décisions "
            + "antérieures à ce champ.")
    private String validatorFullName;

    @Schema(description = "Date de la décision. C'est elle qui ordonne l'historique.")
    private LocalDateTime decisionDate;

    @Schema(description = "Ce qui a été saisi au moment de décider. Vide lorsque l'étape ne "
            + "demandait rien.")
    @Builder.Default
    private List<FieldValueDto> fieldValues = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Une valeur saisie à l'appui d'une décision, conservée avec l'intitulé "
            + "sous lequel elle a été demandée.")
    public static class FieldValueDto {

        @Schema(description = "Identifiant technique du champ d'étape, sous forme de chaîne. Il "
                + "vaut pour une installation donnée : c'est le nom, et non lui, qui se compare "
                + "d'un environnement à l'autre.",
                example = "42")
        private String fieldCode;

        @Schema(description = "Nom technique du champ, seule clé qu'un module métier puisse "
                + "reconnaître.",
                example = "agentImpute")
        private String fieldName;

        /**
         * Intitulé du champ tel qu'il était présenté à qui a saisi. À défaut — valeurs antérieures
         * à sa conservation — le nom technique, seul repère disponible.
         */
        @Schema(description = "Intitulé du champ tel qu'il était présenté à qui a saisi. À défaut "
                + "— valeurs antérieures à sa conservation — le nom technique est rendu à sa "
                + "place, si bien que ce champ n'est jamais vide.",
                example = "Agent chargé du traitement")
        private String fieldLabel;

        @Schema(description = "Valeur saisie, toujours rendue en chaîne quel que soit le type du "
                + "champ. Pour une liste alimentée par un référentiel, c'est l'identifiant retenu.")
        private String value;
    }
}
