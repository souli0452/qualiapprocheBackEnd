package com.qualiapproche.support.mapper;

import com.qualiapproche.support.dto.DocumentWorkflowDto;
import com.qualiapproche.support.dto.WorkflowStepDto;
import com.qualiapproche.support.model.DocumentWorkflow;
import com.qualiapproche.support.model.WorkflowStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    DocumentWorkflowDto toDto(DocumentWorkflow workflow);

    @Mapping(target = "directionId", ignore = true)
    DocumentWorkflow toEntity(DocumentWorkflowDto workflowDto);

    List<DocumentWorkflowDto> toDtos(List<DocumentWorkflow> workflows);

    List<DocumentWorkflow> toEntities(List<DocumentWorkflowDto> workflowDtos);

    WorkflowStepDto toDto(WorkflowStep step);

    @Mapping(target = "workflow", ignore = true)
    WorkflowStep toEntity(WorkflowStepDto stepDto);

    List<WorkflowStepDto> toStepDtos(List<WorkflowStep> steps);

    List<WorkflowStep> toStepEntities(List<WorkflowStepDto> stepDtos);

    @Mapping(target = "directionId", ignore = true)
    void updateEntityFromDto(DocumentWorkflowDto workflowDto, @MappingTarget DocumentWorkflow workflow);

    default DocumentWorkflow map(UUID id) {
        if (id == null) {
            return null;
        }
        DocumentWorkflow workflow = new DocumentWorkflow();
        workflow.setId(id);
        return workflow;
    }
}
