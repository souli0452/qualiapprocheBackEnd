package com.qualiapproche.support.mapper;

import com.qualiapproche.support.dto.DocumentWorkflowDto;
import com.qualiapproche.support.dto.WorkflowStepDto;
import com.qualiapproche.support.dto.WorkflowTransitionDto;
import com.qualiapproche.support.model.DocumentWorkflow;
import com.qualiapproche.support.model.WorkflowStep;
import com.qualiapproche.support.model.WorkflowTransition;
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

    @Mapping(target = "stepTemplateId", source = "stepTemplate.id")
    WorkflowStepDto toDto(WorkflowStep step);

    @org.mapstruct.AfterMapping
    default void fillStepFromTemplate(WorkflowStep step, @MappingTarget WorkflowStepDto dto) {
        if (step != null && step.getStepTemplate() != null) {
            if (step.getStepTemplate().getNomEtape() != null && !step.getStepTemplate().getNomEtape().isBlank()) {
                dto.setNomEtape(step.getStepTemplate().getNomEtape());
            }
            if (step.getStepTemplate().getResponsableRole() != null && !step.getStepTemplate().getResponsableRole().isBlank()) {
                dto.setResponsableRole(step.getStepTemplate().getResponsableRole());
            }
        }
    }

    @Mapping(target = "workflow", ignore = true)
    @Mapping(target = "transitions", ignore = true)
    @Mapping(target = "stepTemplate", ignore = true)
    WorkflowStep toEntity(WorkflowStepDto stepDto);

    List<WorkflowStepDto> toStepDtos(List<WorkflowStep> steps);

    List<WorkflowStep> toStepEntities(List<WorkflowStepDto> stepDtos);

    @Mapping(target = "toStepOrder", source = "toStep.stepOrder")
    WorkflowTransitionDto toDto(WorkflowTransition transition);

    List<WorkflowTransitionDto> toTransitionDtos(List<WorkflowTransition> transitions);

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
