package com.qualiapproche.workflow.event;

import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.AmeliorationWebhookClient;
import com.qualiapproche.workflow.service.SmtpEmailService;
import com.qualiapproche.workflow.service.SupportWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Point unique de propagation d'une transition franchie vers les services métier.
 *
 * <p>Auparavant la notification était éclatée en deux endroits aux règles disjointes :
 * {@code WorkflowStepAction} ne notifiait que sur état terminal, et cet écouteur que les
 * documents. Comme aucun circuit livré ne comporte de transition terminale, les non-conformités
 * et les plans d'action ne recevaient <b>jamais</b> de retour d'état, tandis que le même e-mail
 * partait deux fois sur les étapes intermédiaires. Tout est désormais publié ici, après commit,
 * pour <b>tous</b> les types de ressource et à <b>chaque</b> transition.</p>
 *
 * <p>Le contrat de charge utile est unique et explicite :</p>
 * <ul>
 *   <li>{@code status} — valeur machine : {@code EN_COURS}, {@code APPROVED} ou {@code REJECTED} ;</li>
 *   <li>{@code statusName} — étape atteinte, lisible par un humain (ou la décision si terminal) ;</li>
 *   <li>{@code etatCode} — état de traitement métier de l'étape ({@code VALIDATION_RS}, {@code CLOTURE}…) ;</li>
 *   <li>{@code comments} — commentaire saisi lors de la décision ;</li>
 *   <li>{@code decision} — code de la transition franchie ;</li>
 *   <li>{@code timestamp} — horodatage de la transition.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventListener {

    private static final String INSTANCE_CLASS = WorkflowValidationInstance.class.getName();
    private static final String PREFIXE_ETAT_TERMINAL = "TERMINATED_";

    private final WorkflowStepRepository stepRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final SmtpEmailService emailService;
    private final WorkflowValidationInstanceRepository validationInstanceRepository;
    private final SupportWebhookClient supportWebhookClient;
    private final AmeliorationWebhookClient ameliorationWebhookClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransitionFranchieEvent(TransitionFranchieEvent event) {
        if (!INSTANCE_CLASS.equals(event.getEntityClass()) || event.getEtatApres() == null) {
            return;
        }

        WorkflowValidationInstance instance = chargerInstance(event.getEntityId());
        if (instance == null) {
            return;
        }

        Optional<WorkflowStep> etapeAtteinte = etapeAtteinte(event.getEtatApres());

        notifierServiceMetier(instance, construireCharge(event, etapeAtteinte));
        etapeAtteinte.ifPresent(step -> notifierParEmail(step, event));
    }

    private WorkflowValidationInstance chargerInstance(String entityId) {
        try {
            return validationInstanceRepository.findById(UUID.fromString(entityId)).orElse(null);
        } catch (IllegalArgumentException e) {
            log.warn("Identifiant d'instance de workflow inexploitable : {}", entityId);
            return null;
        }
    }

    /**
     * Étape correspondant à l'état atteint. Vide sur un état terminal synthétique
     * ({@code TERMINATED_*}), qui ne correspond à aucune étape configurée.
     */
    private Optional<WorkflowStep> etapeAtteinte(String etatApres) {
        if (etatApres.startsWith(PREFIXE_ETAT_TERMINAL)) {
            return Optional.empty();
        }
        try {
            return stepRepository.findById(Long.valueOf(etatApres));
        } catch (NumberFormatException e) {
            log.warn("L'état atteint '{}' n'est ni terminal ni un identifiant d'étape.", etatApres);
            return Optional.empty();
        }
    }

    private Map<String, Object> construireCharge(TransitionFranchieEvent event, Optional<WorkflowStep> etapeAtteinte) {
        Map<String, Object> payload = new HashMap<>();

        if (etapeAtteinte.isPresent()) {
            WorkflowStep step = etapeAtteinte.get();
            payload.put("status", "EN_COURS");
            payload.put("statusName", step.getNomEtape());
            payload.put("etatCode", step.getEtatTraitement());
        } else {
            // Etat terminal : le suffixe porte la décision finale (APPROUVE / REJETE).
            String decision = event.getEtatApres().substring(PREFIXE_ETAT_TERMINAL.length());
            payload.put("status", "APPROUVE".equalsIgnoreCase(decision) ? "APPROVED" : "REJECTED");
            payload.put("statusName", decision);
            payload.put("etatCode", null);
        }

        payload.put("comments", event.getCommentaire());
        payload.put("decision", event.getTransitionCode());
        payload.put("timestamp", LocalDateTime.now().toString());
        return payload;
    }

    private void notifierServiceMetier(WorkflowValidationInstance instance, Map<String, Object> payload) {
        UUID resourceId;
        try {
            resourceId = UUID.fromString(instance.getResourceId());
        } catch (IllegalArgumentException e) {
            log.error("Ressource {} non notifiée : identifiant inexploitable.", instance.getResourceId());
            return;
        }

        String resourceType = instance.getResourceType();
        try {
            if ("DOCUMENT".equalsIgnoreCase(resourceType)) {
                supportWebhookClient.updateDocumentStatus(resourceId, payload);
                supportWebhookClient.logDocumentAudit(resourceId, construireAudit(payload));
            } else if ("NON_CONFORMITE".equalsIgnoreCase(resourceType)) {
                ameliorationWebhookClient.updateNonConformiteStatus(resourceId, payload);
            } else if ("PLAN_ACTION".equalsIgnoreCase(resourceType)) {
                ameliorationWebhookClient.updatePlanActionStatus(resourceId, payload);
            } else {
                log.warn("Aucun service destinataire connu pour le type de ressource '{}'.", resourceType);
            }
        } catch (Exception e) {
            // La transition est déjà committée : un échec ici laisse le service métier désynchronisé.
            // À reprendre par un mécanisme de rejeu (outbox) ; tracé en ERROR pour être visible en
            // supervision plutôt que silencieux.
            log.error("Échec de la notification {} de la ressource {} (étape « {} ») : {}",
                    resourceType, resourceId, payload.get("statusName"), e.getMessage(), e);
        }
    }

    private Map<String, Object> construireAudit(Map<String, Object> payload) {
        Object comments = payload.get("comments");
        Map<String, Object> audit = new HashMap<>();
        audit.put("action", "TRANSITION_" + payload.get("decision"));
        audit.put("details", comments != null
                ? comments
                : "Passage à l'étape « " + payload.get("statusName") + " »");
        return audit;
    }

    private void notifierParEmail(WorkflowStep step, TransitionFranchieEvent event) {
        String templateCode = step.getEmailTemplateCode();
        if (templateCode == null || templateCode.isBlank()) {
            return;
        }

        EmailTemplate template = emailTemplateRepository.findByCode(templateCode).orElse(null);
        if (template == null) {
            log.warn("Modèle d'e-mail introuvable pour le code '{}'.", templateCode);
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("subject", template.getSubject());
        variables.put("user", "Responsable " + step.getResponsableRole());
        variables.put("entityId", event.getEntityId());
        variables.put("etatAvant", event.getEtatAvant());
        variables.put("etatApres", step.getNomEtape());
        variables.put("auteurId", event.getAuteurId());
        variables.put("commentaire", event.getCommentaire());

        // TODO destinataires réels : résoudre via user-service les utilisateurs portant
        // step.responsableRole. L'adresse ci-dessous reste un substitut — aucun destinataire
        // réel n'est notifié tant que cette résolution n'est pas faite.
        String destinataire = "role_" + step.getResponsableRole() + "@qualiapproche.com";

        try {
            emailService.sendEmail(destinataire, template.getSubject(), template.getBody(), variables);
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'e-mail '{}' pour l'étape '{}' : {}",
                    templateCode, step.getNomEtape(), e.getMessage());
        }
    }
}
