package com.qualiapproche.workflow.adapter.action;

import com.qualiapproche.workflow.core.action.DefaultTransitionAction;
import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.model.ActionExecutionContext;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.service.AmeliorationWebhookClient;
import com.qualiapproche.workflow.service.SmtpEmailService;
import com.qualiapproche.workflow.service.SupportWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowStepAction extends DefaultTransitionAction<WorkflowValidationInstance, Transition<WorkflowValidationInstance>> {

    private final SupportWebhookClient supportWebhookClient;
    private final AmeliorationWebhookClient ameliorationWebhookClient;
    private final WorkflowStepRepository workflowStepRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final SmtpEmailService smtpEmailService;

    @Override
    protected void update(ActionExecutionContext<WorkflowValidationInstance, Transition<WorkflowValidationInstance>> pContexte) throws WorkflowException {
        super.update(pContexte); // set state

        WorkflowValidationInstance instance = pContexte.getData();
        Etat dest = pContexte.getTransition().getEtatDestination();

        if (dest.getCode().startsWith("TERMINATED_")) {
            instance.setStatus(ValidationStatus.TERMINE);
            instance.setCompletedAt(LocalDateTime.now());
        } else {
            instance.setStatus(ValidationStatus.EN_COURS);
        }
    }

    @Override
    protected void after(ActionExecutionContext<WorkflowValidationInstance, Transition<WorkflowValidationInstance>> pContexte) throws WorkflowException {
        WorkflowValidationInstance instance = pContexte.getData();
        Etat dest = pContexte.getTransition().getEtatDestination();

        // Envoi webhook si état final
        if (dest.getCode().startsWith("TERMINATED_")) {
            String decision = dest.getCode().replace("TERMINATED_", "");
            notifyResourceService(instance.getResourceId(), instance.getResourceType(), decision);
        } else {
            // Etape intermédiaire, envoi email si un template est configuré
            try {
                WorkflowStep nextStep = workflowStepRepository.findById(Long.valueOf(dest.getCode())).orElse(null);
                if (nextStep != null && nextStep.getEmailTemplateCode() != null && !nextStep.getEmailTemplateCode().isEmpty()) {
                    EmailTemplate template = emailTemplateRepository.findByCode(nextStep.getEmailTemplateCode()).orElse(null);
                    if (template != null) {
                        Map<String, String> variables = new HashMap<>();
                        variables.put("subject", template.getSubject());
                        variables.put("user", "Responsable " + nextStep.getResponsableRole()); // Placeholder
                        variables.put("link", "https://qualiapproche.com/resource/" + instance.getResourceId());
                        
                        // Placeholder for TO address based on role
                        String toAddress = "role_" + nextStep.getResponsableRole() + "@qualiapproche.com";
                        
                        smtpEmailService.sendEmail(toAddress, template.getSubject(), template.getBody(), variables);
                    } else {
                        log.warn("EmailTemplate introuvable pour le code: {}", nextStep.getEmailTemplateCode());
                    }
                }
            } catch (NumberFormatException e) {
                log.error("Erreur de parsing de l'ID d'étape depuis l'état: {}", dest.getCode());
            }
        }
    }

    private void notifyResourceService(String resourceId, String resourceType, String decision) {
        Map<String, Object> payload = new HashMap<>();
        // "APPROUVE" -> "APPROVED", "REJETE" -> "REJECTED"
        String status = "APPROUVE".equalsIgnoreCase(decision) ? "APPROVED" : "REJECTED";
        payload.put("status", status);
        payload.put("timestamp", LocalDateTime.now());

        try {
            if ("DOCUMENT".equalsIgnoreCase(resourceType)) {
                supportWebhookClient.updateDocumentStatus(java.util.UUID.fromString(resourceId), payload);
            } else if ("NON_CONFORMITE".equalsIgnoreCase(resourceType)) {
                ameliorationWebhookClient.updateNonConformiteStatus(java.util.UUID.fromString(resourceId), payload);
            } else {
                log.warn("Unknown resourceType for webhook notification: {}", resourceType);
            }
        } catch (Exception e) {
            log.error("Failed to notify external service for resource {}: {}", resourceId, e.getMessage());
        }
    }
}
