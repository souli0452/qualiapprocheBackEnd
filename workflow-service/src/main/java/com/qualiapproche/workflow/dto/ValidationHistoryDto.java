package com.qualiapproche.workflow.dto;

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
public class ValidationHistoryDto {
    private Long id;
    private String stepCode;
    private String stepName;
    private String decision;
    private String comments;
    private String validatorUserId;
    /** Nom de l'auteur au moment de la décision ; nul sur les décisions antérieures à ce champ. */
    private String validatorFullName;
    private LocalDateTime decisionDate;
    @Builder.Default
    private List<FieldValueDto> fieldValues = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldValueDto {
        private String fieldCode;
        private String fieldName;
        /**
         * Intitulé du champ tel qu'il était présenté à qui a saisi. À défaut — valeurs antérieures
         * à sa conservation — le nom technique, seul repère disponible.
         */
        private String fieldLabel;
        private String value;
    }
}
