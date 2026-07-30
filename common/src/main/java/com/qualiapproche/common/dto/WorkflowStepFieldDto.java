package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Champ de saisie exigé par une étape de workflow.
 *
 * <p>Exposé dans {@link WorkflowStateDto} pour que l'appelant puisse construire le formulaire
 * de décision : {@code id} est la clé attendue dans {@code WorkflowValidationRequestDto.fields}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepFieldDto {
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String type;
    private boolean required;
    private String options;
}
