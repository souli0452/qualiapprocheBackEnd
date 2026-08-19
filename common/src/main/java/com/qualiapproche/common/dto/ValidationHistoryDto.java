package com.qualiapproche.common.dto;

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
        /** Intitulé du champ tel qu'il était présenté à qui a saisi ; à défaut, le nom technique. */
        private String fieldLabel;
        private String value;
    }
}
