
package com.qualiapproche.support.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.support.dto.WorkflowStepDto;
import com.qualiapproche.support.dto.DocumentWorkflowDto;
import com.qualiapproche.support.mapper.WorkflowMapper;
import com.qualiapproche.support.model.DocumentWorkflow;
import com.qualiapproche.support.model.WorkflowStep;
import com.qualiapproche.support.model.WorkflowStepTemplate;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.repository.DocumentValidationInstanceRepository;
import com.qualiapproche.support.repository.DocumentWorkflowRepository;
import com.qualiapproche.support.repository.ValidationHistoryRepository;
import com.qualiapproche.support.repository.WorkflowStepTemplateRepository;
import com.qualiapproche.support.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@RequirePermissions(
        create = {"workflow-write"},
        update = {"workflow-write"},
        read = {"workflow-read", "workflow-write"},
        delete = {"workflow-write"},
        validate = {"workflow-validate", "workflow-write"}
)
public class WorkflowController {

    private final WorkflowService workflowService;
    private final DocumentWorkflowRepository workflowRepository;
    private final WorkflowMapper workflowMapper;
    private final DocumentQmsRepository documentRepository;
    private final DocumentValidationInstanceRepository validationInstanceRepository;
    private final ValidationHistoryRepository validationHistoryRepository;
    private final WorkflowStepTemplateRepository stepTemplateRepository;

    @GetMapping
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<DocumentWorkflowDto>> getAllWorkflows() {
        return ResponseEntity.ok(workflowMapper.toDtos(workflowRepository.findAll()));
    }

    @PostMapping
    @PreAuthorize("@perm.canCreate(this)")
    @Transactional
    public ResponseEntity<DocumentWorkflowDto> createWorkflow(@RequestBody DocumentWorkflowDto workflowDto) {
        DocumentWorkflow newWorkflow = workflowMapper.toEntity(workflowDto);
        if (newWorkflow.getSteps() != null && workflowDto.getSteps() != null) {
            List<WorkflowStep> stepEntities = newWorkflow.getSteps();
            List<WorkflowStepDto> stepDtos = workflowDto.getSteps();
            for (int i = 0; i < stepEntities.size() && i < stepDtos.size(); i++) {
                stepEntities.get(i).setWorkflow(newWorkflow);
                stepEntities.get(i).setStepTemplate(resolveStepTemplate(stepDtos.get(i).getStepTemplateId()));
            }
        }
        DocumentWorkflow savedWorkflow = workflowRepository.save(newWorkflow);
        // Les étapes ont désormais un ID : on peut résoudre/construire leurs transitions sortantes
        // (explicites si fournies par le client, sinon défaut séquentiel historique).
        workflowService.syncTransitions(savedWorkflow, workflowDto.getSteps());
        return ResponseEntity.ok(workflowMapper.toDto(workflowRepository.findById(savedWorkflow.getId()).orElseThrow()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.canUpdate(this)")
    @Transactional
    public ResponseEntity<DocumentWorkflowDto> updateWorkflow(@PathVariable UUID id, @RequestBody DocumentWorkflowDto workflowDetails) {
        return workflowRepository.findById(id).map(workflow -> {
            workflow.setNom(workflowDetails.getNom());
            workflow.setDescription(workflowDetails.getDescription());
            workflow.setDocumentType(workflowDetails.getDocumentType());

            if (workflowDetails.getSteps() != null) {
                // Associer les steps existantes par ID
                Map<Long, WorkflowStep> existingStepsMap = workflow.getSteps().stream()
                        .filter(s -> s.getId() != null)
                        .collect(Collectors.toMap(WorkflowStep::getId, s -> s));

                List<WorkflowStep> newSteps = new ArrayList<>();
                for (WorkflowStepDto stepDto : workflowDetails.getSteps()) {
                    if (stepDto.getId() != null && existingStepsMap.containsKey(stepDto.getId())) {
                        // Mise à jour de l'étape existante en place
                        WorkflowStep step = existingStepsMap.get(stepDto.getId());
                        step.setNomEtape(stepDto.getNomEtape());
                        step.setResponsableRole(stepDto.getResponsableRole());
                        step.setDescription(stepDto.getDescription());
                        step.setStepOrder(stepDto.getStepOrder());
                        step.setStepTemplate(resolveStepTemplate(stepDto.getStepTemplateId()));
                        newSteps.add(step);
                    } else {
                        // Nouvelle étape
                        WorkflowStep step = workflowMapper.toEntity(stepDto);
                        step.setWorkflow(workflow);
                        step.setStepTemplate(resolveStepTemplate(stepDto.getStepTemplateId()));
                        newSteps.add(step);
                    }
                }

                // Identifier les étapes supprimées et vérifier si elles sont référencées
                List<WorkflowStep> stepsToRemove = new ArrayList<>();
                for (WorkflowStep oldStep : workflow.getSteps()) {
                    boolean remains = newSteps.stream().anyMatch(ns -> oldStep.getId() != null && oldStep.getId().equals(ns.getId()));
                    if (!remains) {
                        boolean isReferenced = documentRepository.existsByCurrentStepId(oldStep.getId())
                                || validationInstanceRepository.existsByCurrentStepId(oldStep.getId())
                                || validationHistoryRepository.existsByStepId(oldStep.getId());
                        if (isReferenced) {
                            throw new RuntimeException("L'étape '" + oldStep.getNomEtape() + "' est actuellement utilisée par des validations en cours et ne peut pas être supprimée.");
                        }
                        stepsToRemove.add(oldStep);
                    }
                }

                // Détacher les transitions d'AUTRES étapes qui ciblaient une étape supprimée
                // (sinon référence orpheline) : elles deviennent des fins de circuit (toStep = null).
                stepsToRemove.forEach(oldStep -> workflowService.detachStepTransitions(oldStep.getId()));

                // Supprimer et ajouter/garder les étapes
                workflow.getSteps().removeAll(stepsToRemove);
                for (WorkflowStep ns : newSteps) {
                    if (ns.getId() == null || workflow.getSteps().stream().noneMatch(es -> es.getId() != null && es.getId().equals(ns.getId()))) {
                        workflow.getSteps().add(ns);
                    }
                }
            } else {
                workflow.getSteps().clear();
            }

            workflow = workflowRepository.save(workflow);
            workflowService.syncTransitions(workflow, workflowDetails.getSteps());
            return ResponseEntity.ok(workflowMapper.toDto(workflowRepository.findById(workflow.getId()).orElseThrow()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.canDelete(this)")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID id) {
        workflowRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/documents/{documentId}/validate")
    @PreAuthorize("@perm.canValidate(this)")
    public ResponseEntity<Void> validateStep(
            @PathVariable UUID documentId,
            @RequestParam String comments) {
        workflowService.validateStep(documentId, currentUserId(), comments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/{documentId}/reject")
    @PreAuthorize("@perm.canValidate(this)")
    public ResponseEntity<Void> rejectStep(
            @PathVariable UUID documentId,
            @RequestParam String comments) {
        workflowService.rejectStep(documentId, currentUserId(), comments);
        return ResponseEntity.ok().build();
    }

    private WorkflowStepTemplate resolveStepTemplate(UUID templateId) {
        return templateId != null ? stepTemplateRepository.findById(templateId).orElse(null) : null;
    }

    /**
     * L'identité du validateur doit provenir exclusivement du JWT authentifié :
     * un header client (ex. X-User-Id) serait falsifiable et invaliderait la
     * traçabilité des approbations (ValidationHistory).
     */
    private String currentUserId() {
        String userId = com.qualiapproche.common.utils.SecurityUtils.getCurrentUserId();
        return userId != null ? userId : "system";
    }
}
