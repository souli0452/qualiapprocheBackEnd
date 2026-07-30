package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionDto {
    private Long id;
    private String label;
    private String decision;
    private String requiredRole;
    /**
     * Code de l'étape de destination : clé désignant la cible, stable dans le temps et connue de
     * l'appelant avant même l'enregistrement du circuit.
     */
    private String toStepCode;
    private Long toStepId;
    /** Libellé de la destination, pour l'affichage uniquement — jamais utilisé comme clé. */
    private String toStepName;
    /**
     * Rang de l'étape de destination. Exposé en plus de {@code toStepId} parce que les écrans de
     * configuration raisonnent en rang d'étape et non en identifiant technique : sans ce champ,
     * la destination d'une transition ne pouvait pas être restituée.
     */
    private Integer toStepOrder;
}
