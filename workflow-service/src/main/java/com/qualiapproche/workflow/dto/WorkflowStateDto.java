package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStateDto {
    private UUID instanceId;
    private String status;
    private String currentStateCode;
    private String currentStateName;
    @Builder.Default
    private List<WorkflowActionDto> allowedActions = new ArrayList<>();

    /**
     * Champs à saisir pour décider à l'étape courante. Sans eux, l'appelant n'avait aucun moyen
     * de construire le formulaire de validation ni de renseigner
     * {@code WorkflowValidationRequestDto.fields}, qui est indexé par identifiant de champ.
     */
    @Builder.Default
    private List<WorkflowStepFieldDto> currentStepFields = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowActionDto {
        private String code;
        private String libelle;
        private String permission;
        /**
         * Décision portée par la transition ({@code APPROUVE} / {@code REJETE}), pour distinguer
         * une approbation d'un rejet côté présentation.
         */
        private String decision;
    }
}
