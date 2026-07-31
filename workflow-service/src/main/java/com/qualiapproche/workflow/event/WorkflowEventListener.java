package com.qualiapproche.workflow.event;

import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.SmtpEmailService;
import com.qualiapproche.workflow.service.SupportWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventListener {

    private final WorkflowStepRepository stepRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final SmtpEmailService emailService;
    private final WorkflowValidationInstanceRepository validationInstanceRepository;
    private final SupportWebhookClient supportWebhookClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransitionFranchieEvent(TransitionFranchieEvent event) {
        log.info("Traitement de l'événement TransitionFranchieEvent pour l'entité {} avec ID {}", event.getEntityClass(), event.getEntityId());

        if (event.getEtatApres() == null) {
            return;
        }

        try {
            Long stepId = Long.valueOf(event.getEtatApres());
            stepRepository.findById(stepId).ifPresent(step -> {
                
                // Send audit and status webhooks to support-service if it's a DOCUMENT validation instance
                if ("com.qualiapproche.workflow.model.WorkflowValidationInstance".equals(event.getEntityClass())) {
                    try {
                        java.util.UUID instanceId = java.util.UUID.fromString(event.getEntityId());
                        validationInstanceRepository.findById(instanceId).ifPresent(instance -> {
                            if ("DOCUMENT".equalsIgnoreCase(instance.getResourceType())) {
                                try {
                                    java.util.UUID resourceId = java.util.UUID.fromString(instance.getResourceId());
                                    
                                    // 1. Update document status
                                    Map<String, Object> statusPayload = new HashMap<>();
                                    statusPayload.put("status", step.getNomEtape());
                                    statusPayload.put("comments", event.getCommentaire());
                                    
                                    log.info("Sending workflow status update to support-service for document {}: {}", resourceId, step.getNomEtape());
                                    supportWebhookClient.updateDocumentStatus(resourceId, statusPayload);
                                    
                                    // 2. Log document audit
                                    Map<String, Object> auditPayload = new HashMap<>();
                                    auditPayload.put("action", "TRANSITION_" + event.getTransitionCode());
                                    auditPayload.put("details", event.getCommentaire() != null ? event.getCommentaire() : "Action exécutée");
                                    
                                    log.info("Sending audit webhook to support-service for document {}", resourceId);
                                    supportWebhookClient.logDocumentAudit(resourceId, auditPayload);
                                } catch (Exception e) {
                                    log.warn("Failed to send workflow webhook for document {}: {}", instance.getResourceId(), e.getMessage());
                                }
                            }
                        });
                    } catch (Exception e) {
                        log.warn("Could not parse entityId as UUID: {}", event.getEntityId());
                    }
                }

                // Send Email Notification
                String templateCode = step.getEmailTemplateCode();
                if (templateCode != null && !templateCode.trim().isEmpty()) {
                    emailTemplateRepository.findByCode(templateCode).ifPresent(template -> {
                        Map<String, String> variables = new HashMap<>();
                        variables.put("entityId", event.getEntityId());
                        variables.put("entityClass", event.getEntityClass());
                        variables.put("etatAvant", event.getEtatAvant());
                        variables.put("etatApres", step.getNomEtape());
                        variables.put("auteurId", event.getAuteurId());
                        variables.put("commentaire", event.getCommentaire());
                        
                        // TODO: Récupérer l'adresse email réelle des destinataires (par ex: via responsableRole)
                        String toEmail = "test@example.com"; 
                        log.info("Envoi de l'email pour le template {} vers {}", templateCode, toEmail);
                        
                        emailService.sendEmail(toEmail, template.getSubject(), template.getBody(), variables);
                    });
                }
            });
        } catch (NumberFormatException e) {
            log.warn("etatApres n'est pas un ID numérique valide: {}", event.getEtatApres());
        }
    }
}
