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
    private final QmsDocumentVersionRepository versionRepository;

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
        document.setCurrentStep(firstStep);
        document.setEsTraiter(false);
        document.setObsolete(false);
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

        // Vérification de sécurité : l'utilisateur doit avoir le rôle requis ou être ADMIN/MANAGE
        if (currentStep.getResponsableRole() != null && !currentStep.getResponsableRole().isBlank()) {
            boolean hasRequiredRole = com.qualiapproche.common.utils.SecurityUtils.hasRole(currentStep.getResponsableRole());
            boolean isAdmin = com.qualiapproche.common.utils.SecurityUtils.hasRole("ADMIN") || com.qualiapproche.common.utils.SecurityUtils.hasRole("MANAGE");
            if (!hasRequiredRole && !isAdmin) {
                log.warn("User {} has insufficient permissions to validate step {} (required role: {})", 
                        userId, currentStep.getNomEtape(), currentStep.getResponsableRole());
                throw new org.springframework.security.access.AccessDeniedException(
                        "Vous n'avez pas le rôle requis (" + currentStep.getResponsableRole() + ") pour valider cette étape."
                );
            }
        }

        // Récupération de l'empreinte SHA-256 de la version en cours de validation
        List<QmsDocumentVersion> versions = versionRepository.findByDocumentIdOrderByDateCreationDesc(document.getId());
        String documentHash = (versions != null && !versions.isEmpty()) ? versions.get(0).getFileHash() : null;

        // Historique
        ValidationHistory history = ValidationHistory.builder()
                .validationInstance(instance)
                .step(currentStep)
                .decision(decision)
                .comments(comments)
                .validatorUserId(userId)
                .decisionDate(LocalDateTime.now())
                .documentHash(documentHash)
                .build();
        historyRepository.save(history);

        // Récupération de l'ordre des étapes pour trouver la précédente ou suivante
        List<WorkflowStep> steps = instance.getWorkflow().getSteps();
        int currentIndex = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(currentStep.getId())) {
                currentIndex = i;
                break;
            }
        }

        if (decision == StepDecision.REJETE) {
            if (currentIndex > 0) {
                // Revenir à l'étape précédente
                WorkflowStep prevStep = steps.get(currentIndex - 1);
                instance.setCurrentStep(prevStep);
                document.setCurrentStep(prevStep);
                
                auditLogService.logAction(
                    "REJET_ETAPE", 
                    document.getDocumentNumber(), 
                    "Rejeté à l'étape: " + currentStep.getNomEtape() + " par " + userId + ". Retour à l'étape précédente: " + prevStep.getNomEtape() + ". Commentaire: " + comments
                );
            } else {
                // Si c'est la première étape, on repasse en brouillon (step = null)
                instance.setStatus(ValidationStatus.TERMINE);
                instance.setCurrentStep(null);
                instance.setCompletedAt(LocalDateTime.now());
                
                document.setCurrentStep(null);
                document.setWorkflowStatus("TERMINE");
                
                auditLogService.logAction(
                    "REJET_ETAPE", 
                    document.getDocumentNumber(), 
                    "Rejeté à la première étape: " + currentStep.getNomEtape() + " par " + userId + ". Retour en brouillon. Commentaire: " + comments
                );
            }
        } else {
            // Approuvé
            auditLogService.logAction(
                "APPROBATION_ETAPE", 
                document.getDocumentNumber(), 
                "Approuvé à l'étape: " + currentStep.getNomEtape() + " par " + userId + ". Commentaire: " + comments
            );

            if (currentIndex != -1 && currentIndex + 1 < steps.size()) {
                // Étape suivante
                WorkflowStep nextStep = steps.get(currentIndex + 1);
                instance.setCurrentStep(nextStep);
                document.setCurrentStep(nextStep);

                // Increment version on step validation
                document.setVersionMajeure(document.getVersionMajeure() + 1);
                document.setVersionMineure(0);
            } else {
                // Terminé (toutes les étapes validées)
                instance.setStatus(ValidationStatus.TERMINE);
                instance.setCurrentStep(null);
                instance.setCompletedAt(LocalDateTime.now());
                
                document.setCurrentStep(null);
                document.setEsTraiter(true);
                document.setEnRetardRevision(false);
                document.setWorkflowStatus("TERMINE");
                document.setDateVigueur(LocalDateTime.now());
                if (document.getPeriodiciteMois() != null) {
                    document.setDateProchRevision(document.getDateVigueur().plusMonths(document.getPeriodiciteMois()));
                }

                // Increment version on final step validation
                document.setVersionMajeure(document.getVersionMajeure() + 1);
                document.setVersionMineure(0);
                
                auditLogService.logAction(
                    "WORKFLOW_TERMINE", 
                    document.getDocumentNumber(), 
                    "Toutes les étapes de validation ont été approuvées. Le document est maintenant TRAITE."
                );
            }
        }

        validationInstanceRepository.save(instance);
        documentRepository.save(document);
    }
}
