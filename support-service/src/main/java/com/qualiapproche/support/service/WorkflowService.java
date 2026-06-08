package com.qualiapproche.support.service;

import com.qualiapproche.support.model.*;
import com.qualiapproche.support.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final DocumentWorkflowRepository workflowRepository;
    private final DocumentValidationInstanceRepository validationInstanceRepository;
    private final ValidationHistoryRepository historyRepository;
    private final DocumentQmsRepository documentRepository;
    private final QmsAuditLogService auditLogService;

    @Transactional
    public DocumentValidationInstance initiateWorkflow(DocumentQms document, UUID workflowId) {
        DocumentWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow introuvable"));

        if (workflow.getSteps().isEmpty()) {
            throw new RuntimeException("Ce workflow n'a aucune étape configurée");
        }

        WorkflowStep firstStep = workflow.getSteps().get(0);

        DocumentValidationInstance instance = DocumentValidationInstance.builder()
                .document(document)
                .workflow(workflow)
                .currentStep(firstStep)
                .status(ValidationStatus.EN_COURS)
                .startedAt(LocalDateTime.now())
                .build();

        instance = validationInstanceRepository.save(instance);
        
        document.setWorkflowStatus("EN_COURS");
        documentRepository.save(document);

        return instance;
    }

    @Transactional
    public void validateStep(UUID documentId, String userId, String comments) {
        processStepDecision(documentId, userId, comments, StepDecision.APPROUVE);
    }

    @Transactional
    public void rejectStep(UUID documentId, String userId, String comments) {
        processStepDecision(documentId, userId, comments, StepDecision.REJETE);
    }

    private void processStepDecision(UUID documentId, String userId, String comments, StepDecision decision) {
        DocumentValidationInstance instance = validationInstanceRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Instance de validation introuvable pour ce document"));

        if (instance.getStatus() != ValidationStatus.EN_COURS || instance.getCurrentStep() == null) {
            throw new RuntimeException("Le document n'est pas en cours de validation");
        }

        WorkflowStep currentStep = instance.getCurrentStep();
        DocumentQms document = instance.getDocument();

        // Historique
        ValidationHistory history = ValidationHistory.builder()
                .validationInstance(instance)
                .step(currentStep)
                .decision(decision)
                .comments(comments)
                .validatorUserId(userId)
                .decisionDate(LocalDateTime.now())
                .build();
        historyRepository.save(history);

        if (decision == StepDecision.REJETE) {
            instance.setStatus(ValidationStatus.TERMINE);
            instance.setCurrentStep(null);
            instance.setCompletedAt(LocalDateTime.now());
            
            document.setStatus("brouillon"); // Retour au brouillon sur rejet
            document.setWorkflowStatus("TERMINE");
            
            auditLogService.logAction(
                "REJET_ETAPE", 
                document.getDocumentNumber(), 
                "Rejeté à l'étape: " + currentStep.getNomEtape() + " par " + userId + ". Commentaire: " + comments
            );
        } else {
            // Approuvé
            auditLogService.logAction(
                "APPROBATION_ETAPE", 
                document.getDocumentNumber(), 
                "Approuvé à l'étape: " + currentStep.getNomEtape() + " par " + userId + ". Commentaire: " + comments
            );

            // Check if next step exists
            List<WorkflowStep> steps = instance.getWorkflow().getSteps();
            int currentIndex = -1;
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).getId().equals(currentStep.getId())) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex != -1 && currentIndex + 1 < steps.size()) {
                // Next step
                instance.setCurrentStep(steps.get(currentIndex + 1));
            } else {
                // Terminé (toutes les étapes validées)
                instance.setStatus(ValidationStatus.TERMINE);
                instance.setCurrentStep(null);
                instance.setCompletedAt(LocalDateTime.now());
                
                document.setStatus("valide");
                document.setWorkflowStatus("TERMINE");
                document.setDateVigueur(LocalDateTime.now());
                if (document.getPeriodiciteMois() != null) {
                    document.setDateProchRevision(document.getDateVigueur().plusMonths(document.getPeriodiciteMois()));
                }
                
                auditLogService.logAction(
                    "WORKFLOW_TERMINE", 
                    document.getDocumentNumber(), 
                    "Toutes les étapes de validation ont été approuvées. Le document est maintenant VALIDE."
                );
            }
        }

        validationInstanceRepository.save(instance);
        documentRepository.save(document);
    }
}
