package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.dto.*;
import com.qualiapproche.workflow.model.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class WorkflowMapper {

    public WorkflowDto toDto(Workflow entity) {
        if (entity == null) return null;
        return WorkflowDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .resourceType(entity.getResourceType())
                .actif(entity.isActif())
                .steps(entity.getSteps() != null ? entity.getSteps().stream().map(this::toDto).collect(Collectors.toList()) : null)
                .build();
    }

    public WorkflowStepDto toDto(WorkflowStep entity) {
        if (entity == null) return null;
        return WorkflowStepDto.builder()
                .id(entity.getId())
                .nomEtape(entity.getNomEtape())
                .stepOrder(entity.getStepOrder())
                .responsableRole(entity.getResponsableRole())
                .description(entity.getDescription())
                .etatTraitement(entity.getEtatTraitement())
                .emailTemplateCode(entity.getEmailTemplateCode())
                .transitions(entity.getTransitions() != null ? entity.getTransitions().stream().map(this::toDto).collect(Collectors.toList()) : null)
                .fields(entity.getFields() != null ? entity.getFields().stream().map(this::toDto).collect(Collectors.toList()) : null)
                .build();
    }

    public WorkflowTransitionDto toDto(WorkflowTransition entity) {
        if (entity == null) return null;
        return WorkflowTransitionDto.builder()
                .id(entity.getId())
                .label(entity.getLabel())
                .decision(entity.getDecision() != null ? entity.getDecision().name() : null)
                .requiredRole(entity.getRequiredRole())
                .toStepId(entity.getToStep() != null ? entity.getToStep().getId() : null)
                .toStepName(entity.getToStep() != null ? entity.getToStep().getNomEtape() : null)
                .toStepOrder(entity.getToStep() != null ? entity.getToStep().getStepOrder() : null)
                .build();
    }

    public WorkflowStepFieldDto toDto(WorkflowStepField entity) {
        if (entity == null) return null;
        return WorkflowStepFieldDto.builder()
                .id(entity.getId())
                .fieldName(entity.getFieldName())
                .fieldLabel(entity.getFieldLabel())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .required(entity.isRequired())
                .options(entity.getOptions())
                .build();
    }

    public Workflow toEntity(WorkflowDto dto) {
        if (dto == null) return null;
        Workflow workflow = Workflow.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .description(dto.getDescription())
                .resourceType(dto.getResourceType())
                .actif(dto.isActif())
                .build();

        if (dto.getSteps() != null) {
            workflow.setSteps(dto.getSteps().stream().map(stepDto -> {
                WorkflowStep step = toEntity(stepDto);
                step.setWorkflow(workflow);
                return step;
            }).collect(Collectors.toList()));
        }
        return workflow;
    }

    public WorkflowStep toEntity(WorkflowStepDto dto) {
        if (dto == null) return null;
        WorkflowStep step = WorkflowStep.builder()
                .id(dto.getId())
                .nomEtape(dto.getNomEtape())
                .stepOrder(dto.getStepOrder())
                .responsableRole(dto.getResponsableRole())
                .description(dto.getDescription())
                .etatTraitement(dto.getEtatTraitement())
                .emailTemplateCode(dto.getEmailTemplateCode())
                .build();
                
        if (dto.getTransitions() != null) {
            step.setTransitions(dto.getTransitions().stream().map(transitionDto -> {
                WorkflowTransition transition = toEntity(transitionDto);
                transition.setFromStep(step);
                return transition;
            }).collect(Collectors.toList()));
        }
        
        if (dto.getFields() != null) {
            step.setFields(dto.getFields().stream().map(fieldDto -> {
                WorkflowStepField field = toEntity(fieldDto);
                field.setStep(step);
                return field;
            }).collect(Collectors.toList()));
        }
        
        return step;
    }

    public WorkflowTransition toEntity(WorkflowTransitionDto dto) {
        if (dto == null) return null;
        return WorkflowTransition.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .decision(dto.getDecision() != null ? StepDecision.valueOf(dto.getDecision()) : null)
                .requiredRole(dto.getRequiredRole())
                // Note: toStep mapping needs to be resolved by ID later in the service or by a lookup
                .build();
    }

    public WorkflowStepField toEntity(WorkflowStepFieldDto dto) {
        if (dto == null) return null;
        return WorkflowStepField.builder()
                .id(dto.getId())
                .fieldName(dto.getFieldName())
                .fieldLabel(dto.getFieldLabel())
                .type(dto.getType() != null ? FieldType.valueOf(dto.getType()) : null)
                .isRequired(dto.isRequired())
                .options(dto.getOptions())
                .build();
    }
}
