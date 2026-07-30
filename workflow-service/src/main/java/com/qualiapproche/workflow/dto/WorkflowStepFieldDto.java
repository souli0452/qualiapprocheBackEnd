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
}
