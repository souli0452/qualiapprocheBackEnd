package com.qualiapproche.support.service;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.support.dto.WorkflowStepDto;
import com.qualiapproche.support.dto.WorkflowTransitionDto;
import com.qualiapproche.support.model.*;
import com.qualiapproche.support.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final WorkflowTransitionRepository transitionRepository;

    @Transactional
    public DocumentValidationInstance initiateWorkflow(DocumentQms document, UUID workflowId) {
        DocumentWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow introuvable"));

        if (workflow.getSteps().isEmpty()) {
            throw new RuntimeException("Ce workflow n'a aucune étape configurée");
        }

        // Point d'entrée du circuit = étape de plus petit stepOrder (liste triée par @OrderBy).
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

    /**
     * Fait avancer l'instance de validation en cours selon la transition explicitement
     * configurée pour (étape courante, décision). Le routage ne dépend plus de l'ordre des
     * étapes au moment de l'appel : il est entièrement déterminé par {@link WorkflowTransition}.
     */
    private void processStepDecision(UUID documentId, String userId, String comments, StepDecision decision) {
        DocumentValidationInstance instance = validationInstanceRepository
                .findByDocumentIdAndStatus(documentId, ValidationStatus.EN_COURS)
                .orElseThrow(() -> new RuntimeException("Instance de validation introuvable pour ce document"));

        if (instance.getCurrentStep() == null) {
            throw new RuntimeException("Le document n'est pas en cours de validation");
        }

        WorkflowStep currentStep = instance.getCurrentStep();
        DocumentQms document = instance.getDocument();

        WorkflowTransition transition = transitionRepository
                .findByFromStepIdAndDecision(currentStep.getId(), decision)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucune transition n'est configurée pour la décision " + decision +
                                " à l'étape '" + currentStep.getNomEtape() + "'. Circuit mal configuré."));

        // Le rôle requis pour DÉCLENCHER CETTE TRANSITION précise peut surcharger celui de l'étape
        // (ex: une transition de rejet-escalade réservée à un rôle différent de l'approbation simple).
        String requiredRole = (transition.getRequiredRole() != null && !transition.getRequiredRole().isBlank())
                ? transition.getRequiredRole()
                : currentStep.getResponsableRole();

        if (requiredRole != null && !requiredRole.isBlank()) {
            boolean hasRequiredRole = SecurityUtils.hasRole(requiredRole);
            boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGE");
            if (!hasRequiredRole && !isAdmin) {
                log.warn("User {} has insufficient permissions to trigger transition '{}' at step {} (required role: {})",
                        userId, decision, currentStep.getNomEtape(), requiredRole);
                throw new AccessDeniedException(
                        "Vous n'avez pas le rôle requis (" + requiredRole + ") pour cette action sur cette étape."
                );
            }
        }

        // Empreinte SHA-256 de la version en cours de validation, figée dans l'historique
        List<QmsDocumentVersion> versions = versionRepository.findByDocumentIdOrderByDateCreationDesc(document.getId());
        String documentHash = (versions != null && !versions.isEmpty()) ? versions.get(0).getFileHash() : null;

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

        WorkflowStep toStep = transition.getToStep();

        if (toStep != null) {
            // Déplacement explicite vers l'étape cible (en avant ou en arrière selon le circuit configuré)
            instance.setCurrentStep(toStep);
            document.setCurrentStep(toStep);

            auditLogService.logAction(
                    decision == StepDecision.APPROUVE ? "APPROBATION_ETAPE" : "REJET_ETAPE",
                    document.getDocumentNumber(),
                    (decision == StepDecision.APPROUVE ? "Approuvé" : "Rejeté") + " à l'étape: " + currentStep.getNomEtape() +
                            " par " + userId + ". Étape suivante: " + toStep.getNomEtape() + ". Commentaire: " + comments
            );
        } else if (decision == StepDecision.APPROUVE) {
            // Fin de circuit par validation finale
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

            // La version majeure n'avance qu'une seule fois, à la publication effective du document.
            document.setVersionMajeure(document.getVersionMajeure() + 1);
            document.setVersionMineure(0);

            auditLogService.logAction(
                    "APPROBATION_ETAPE", document.getDocumentNumber(),
                    "Approuvé à l'étape: " + currentStep.getNomEtape() + " par " + userId + ". Commentaire: " + comments
            );
            auditLogService.logAction(
                    "WORKFLOW_TERMINE", document.getDocumentNumber(),
                    "Toutes les étapes de validation ont été approuvées. Le document est maintenant TRAITE."
            );
        } else {
            // Fin de circuit par rejet sans étape de repli configurée : retour en brouillon
            instance.setStatus(ValidationStatus.TERMINE);
            instance.setCurrentStep(null);
            instance.setCompletedAt(LocalDateTime.now());

            document.setCurrentStep(null);
            document.setWorkflowStatus(null);

            auditLogService.logAction(
                    "REJET_ETAPE", document.getDocumentNumber(),
                    "Rejeté à l'étape: " + currentStep.getNomEtape() + " par " + userId + ". Retour en brouillon. Commentaire: " + comments
            );
        }

        validationInstanceRepository.save(instance);
        documentRepository.save(document);
    }

    /**
     * Construit ou met à jour les transitions sortantes des étapes d'un workflow déjà persisté
     * (les étapes doivent avoir un ID, donc appeler après la sauvegarde du workflow).
     *
     * Pour chaque étape et chaque décision (APPROUVE/REJETE) :
     * - si le client a fourni une transition explicite (via {@code stepDtos}), elle est appliquée
     *   telle quelle (y compris pour la remplacer si elle existait déjà) ;
     * - sinon, si aucune transition n'existe encore pour ce couple (étape, décision), on applique
     *   le comportement séquentiel historique (étape suivante/précédente par stepOrder) ;
     * - sinon (transition déjà configurée et non fournie par le client), on ne touche à rien —
     *   une mise à jour partielle ne doit jamais écraser un circuit personnalisé existant.
     */
    @Transactional
    public void syncTransitions(DocumentWorkflow workflow, List<WorkflowStepDto> stepDtos) {
        List<WorkflowStep> steps = workflow.getSteps(); // trié par stepOrder ASC (@OrderBy), IDs déjà assignés

        Map<Integer, WorkflowStep> stepByOrder = new HashMap<>();
        for (WorkflowStep step : steps) {
            stepByOrder.put(step.getStepOrder(), step);
        }

        Map<String, WorkflowTransitionDto> explicitTransitions = new HashMap<>();
        if (stepDtos != null) {
            for (WorkflowStepDto stepDto : stepDtos) {
                if (stepDto.getTransitions() == null) continue;
                for (WorkflowTransitionDto tDto : stepDto.getTransitions()) {
                    if (tDto.getDecision() == null) continue;
                    explicitTransitions.put(stepDto.getStepOrder() + "#" + tDto.getDecision(), tDto);
                }
            }
        }

        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            for (StepDecision decision : StepDecision.values()) {
                WorkflowTransitionDto explicitDto = explicitTransitions.get(step.getStepOrder() + "#" + decision);
                Optional<WorkflowTransition> existing = transitionRepository
                        .findByFromStepIdAndDecision(step.getId(), decision);

                if (explicitDto == null && existing.isPresent()) {
                    continue; // rien de fourni par le client : on conserve le circuit déjà configuré
                }

                WorkflowStep resolvedTarget;
                String requiredRole = null;
                String label = null;

                if (explicitDto != null) {
                    if (explicitDto.getToStepOrder() != null) {
                        resolvedTarget = stepByOrder.get(explicitDto.getToStepOrder());
                        if (resolvedTarget == null) {
                            throw new IllegalStateException("Étape cible introuvable (stepOrder=" +
                                    explicitDto.getToStepOrder() + ") pour la transition '" + decision +
                                    "' depuis '" + step.getNomEtape() + "'.");
                        }
                    } else {
                        resolvedTarget = null;
                    }
                    requiredRole = explicitDto.getRequiredRole();
                    label = explicitDto.getLabel();
                } else {
                    // Défaut séquentiel historique
                    resolvedTarget = decision == StepDecision.APPROUVE
                            ? (i + 1 < steps.size() ? steps.get(i + 1) : null)
                            : (i > 0 ? steps.get(i - 1) : null);
                }

                WorkflowTransition transition = existing.orElseGet(() -> WorkflowTransition.builder()
                        .fromStep(step)
                        .decision(decision)
                        .build());
                transition.setToStep(resolvedTarget);
                transition.setRequiredRole(requiredRole);
                transition.setLabel(label);
                transitionRepository.save(transition);
            }
        }
    }

    /**
     * Filet de sécurité au démarrage : les workflows créés avant l'introduction des transitions
     * explicites n'ont aucune ligne dans {@code qms_workflow_transition}. On complète alors le
     * comportement séquentiel historique pour qu'aucun circuit existant ne se retrouve bloqué
     * avec une décision sans transition configurée. Idempotent : ne touche jamais une transition
     * déjà définie (via {@link #syncTransitions}), donc sans risque à rejouer à chaque démarrage.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillMissingTransitions() {
        List<DocumentWorkflow> workflows = workflowRepository.findAll();
        for (DocumentWorkflow workflow : workflows) {
            if (!workflow.getSteps().isEmpty()) {
                syncTransitions(workflow, null);
            }
        }
        log.info("Vérification des transitions de workflow au démarrage : {} circuit(s) contrôlé(s).", workflows.size());
    }

    /**
     * Détache proprement une étape avant sa suppression : toute transition d'une AUTRE étape
     * qui la ciblait est ramenée à "fin de circuit" (toStep = null) plutôt que de laisser une
     * référence orpheline ou de bloquer la sauvegarde.
     */
    @Transactional
    public void detachStepTransitions(Long stepId) {
        List<WorkflowTransition> incoming = transitionRepository.findByToStepId(stepId);
        for (WorkflowTransition t : incoming) {
            t.setToStep(null);
            transitionRepository.save(t);
        }
    }
}