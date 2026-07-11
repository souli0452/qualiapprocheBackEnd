package com.qualiapproche.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepDto{
    private Long id;
    private String nomEtape;
    private int stepOrder;
    private String responsableRole;
    private String description;
}
