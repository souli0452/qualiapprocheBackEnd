package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepFieldDto {
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String type; // string, numeric, select, file, date
    private boolean required;
    private String options;

    /**
     * Décision à laquelle le champ se rapporte ({@code APPROUVE}, {@code REJETE}), ou {@code null}
     * s'il vaut quelle que soit la décision. L'écran n'a ainsi à présenter que ce que la décision
     * choisie réclame.
     */
    private String decision;
}
