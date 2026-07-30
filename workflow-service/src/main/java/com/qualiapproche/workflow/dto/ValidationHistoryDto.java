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
        private String value;
    }
}
