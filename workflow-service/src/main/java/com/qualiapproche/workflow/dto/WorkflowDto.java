package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDto {
    private UUID id;
    private String nom;
    private String description;
    private String resourceType;
    @Builder.Default
    private boolean actif = true;
    @Builder.Default
    private List<WorkflowStepDto> steps = new ArrayList<>();
}
