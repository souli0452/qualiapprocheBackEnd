package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.dto.WorkflowStepFieldDto;
import com.qualiapproche.workflow.dto.WorkflowTransitionDto;
import com.qualiapproche.workflow.model.FieldType;
import com.qualiapproche.workflow.model.SeveriteAction;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.WorkflowTransition;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class WorkflowMapper {

    public WorkflowDto toDto(Workflow entity) {
        if (entity == null) {
            return null;
        }
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
        if (entity == null) {
            return null;
        }
        return WorkflowStepDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nomEtape(entity.getNomEtape())
                .stepOrder(entity.getStepOrder())
                .responsableRole(entity.getResponsableRole())
                .description(entity.getDescription())
                .etatTraitement(entity.getEtatTraitement())
                .emailTemplateCode(entity.getEmailTemplateCode())
                .stepTemplateId(entity.getStepTemplateId())
                .champTitulaire(entity.getChampTitulaire())
                .transitions(entity.getTransitions() != null ? entity.getTransitions().stream().map(this::toDto).collect(Collectors.toList()) : null)
                .fields(entity.getFields() != null ? entity.getFields().stream().map(this::toDto).collect(Collectors.toList()) : null)
                .build();
    }

    public WorkflowTransitionDto toDto(WorkflowTransition entity) {
        if (entity == null) {
            return null;
        }
        return WorkflowTransitionDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .icon(entity.getIcon())
                .severity(entity.getSeverity() != null ? entity.getSeverity().getCode() : null)
                .decision(entity.getDecision() != null ? entity.getDecision().name() : null)
                .requiredRole(entity.getRequiredRole())
                .toStepCode(entity.getToStep() != null ? entity.getToStep().getCode() : null)
                .toStepId(entity.getToStep() != null ? entity.getToStep().getId() : null)
                .toStepName(entity.getToStep() != null ? entity.getToStep().getNomEtape() : null)
                .toStepOrder(entity.getToStep() != null ? entity.getToStep().getStepOrder() : null)
                .terminal(entity.isTerminal())
                .conditionRequise(entity.getConditionRequise())
                .conditionLibelle(entity.getConditionLibelle())
                .build();
    }

    public WorkflowStepFieldDto toDto(WorkflowStepField entity) {
        if (entity == null) {
            return null;
        }
        return WorkflowStepFieldDto.builder()
                .id(entity.getId())
                .fieldName(entity.getFieldName())
                .fieldLabel(entity.getFieldLabel())
                .type(entity.getType() != null ? entity.getType().name() : null)
                .required(entity.isRequired())
                .options(entity.getOptions())
                .decision(entity.getDecision() != null ? entity.getDecision().name() : null)
                .actionCode(entity.getActionCode())
                .build();
    }

    public Workflow toEntity(WorkflowDto dto) {
        if (dto == null) {
            return null;
        }
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
        if (dto == null) {
            return null;
        }
        WorkflowStep step = WorkflowStep.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .nomEtape(dto.getNomEtape())
                .stepOrder(dto.getStepOrder())
                .responsableRole(dto.getResponsableRole())
                .description(dto.getDescription())
                .etatTraitement(dto.getEtatTraitement())
                .emailTemplateCode(dto.getEmailTemplateCode())
                .stepTemplateId(dto.getStepTemplateId())
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

    /**
     * Couleur de bouton, signalée en 400 explicite plutôt qu'en erreur serveur si le jeton est
     * inconnu. Partagé par la création — qui passe par ce mapper — et par la modification, qui
     * fusionne les transitions dans {@code WorkflowService} : une faute de frappe dans l'écran de
     * configuration doit se lire pareil des deux côtés.
     */
    static SeveriteAction severiteValide(String severite) {
        try {
            return SeveriteAction.depuis(severite);
        } catch (IllegalArgumentException e) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    e.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Fait exigé, ramené à la forme sous laquelle les modules métier le déclarent.
     *
     * <p>Un fait saisi avec une casse ou des espaces différents ne serait jamais reconnu : la
     * transition resterait fermée sans que rien ne le signale, et l'on chercherait la faute du
     * côté du module qui déclare pourtant bien son fait. La chaîne vide vaut « aucune
     * condition » — et non « un fait sans nom », qui bloquerait la transition à jamais.</p>
     */
    static String normaliserFait(String fait) {
        if (fait == null || fait.isBlank()) {
            return null;
        }
        return fait.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Code d'action ramené à sa forme de référence : sans espaces superflus, en majuscules.
     *
     * <p>Même raison que pour les faits : le code sert de clé — appariement d'une transition lors
     * d'une modification du circuit, rattachement d'un champ à une action — et deux graphies
     * différentes du même code créeraient silencieusement deux actions là où l'auteur n'en voyait
     * qu'une.</p>
     */
    static String normaliserCodeAction(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
    }

    public WorkflowTransition toEntity(WorkflowTransitionDto dto) {
        if (dto == null) {
            return null;
        }
        return WorkflowTransition.builder()
                .id(dto.getId())
                .code(normaliserCodeAction(dto.getCode()))
                .label(dto.getLabel())
                .icon(dto.getIcon())
                .severity(severiteValide(dto.getSeverity()))
                .decision(dto.getDecision() != null ? StepDecision.valueOf(dto.getDecision()) : null)
                .requiredRole(dto.getRequiredRole())
                .terminal(dto.isTerminal())
                .conditionRequise(normaliserFait(dto.getConditionRequise()))
                .conditionLibelle(dto.getConditionLibelle())
                // Note: toStep mapping needs to be resolved by ID later in the service or by a lookup
                .build();
    }

    public WorkflowStepField toEntity(WorkflowStepFieldDto dto) {
        if (dto == null) {
            return null;
        }
        return WorkflowStepField.builder()
                .id(dto.getId())
                .fieldName(dto.getFieldName())
                .fieldLabel(dto.getFieldLabel())
                .type(dto.getType() != null ? FieldType.valueOf(dto.getType()) : null)
                .isRequired(dto.isRequired())
                .options(dto.getOptions())
                .decision(dto.getDecision() != null && !dto.getDecision().isBlank()
                        ? StepDecision.valueOf(dto.getDecision().toUpperCase()) : null)
                .actionCode(normaliserCodeAction(dto.getActionCode()))
                .build();
    }
}
