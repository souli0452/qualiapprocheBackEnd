package com.qualiapproche.support.dto;

import com.qualiapproche.common.dto.AuditEntityDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentWorkflowDto extends AuditEntityDto {

    private String nom;
    private String description;
    private String documentType;
    private List<WorkflowStepDto> steps;
}
