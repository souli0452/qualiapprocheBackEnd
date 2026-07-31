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
    private Long toStepId; // We only need the ID to map it back or just the stepOrder
    private String toStepName;
}
