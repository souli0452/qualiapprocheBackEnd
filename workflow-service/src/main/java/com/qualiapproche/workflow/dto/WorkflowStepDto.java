package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepDto {
    private Long id;
    private String nomEtape;
    private int stepOrder;
    private String responsableRole;
    private String description;
    private String etatTraitement;
    private String emailTemplateCode;
    @Builder.Default
    private List<WorkflowTransitionDto> transitions = new ArrayList<>();
    @Builder.Default
    private List<WorkflowStepFieldDto> fields = new ArrayList<>();
}
