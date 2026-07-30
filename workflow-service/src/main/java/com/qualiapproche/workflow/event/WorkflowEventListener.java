package com.qualiapproche.workflow.event;

import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.SmtpEmailService;
import com.qualiapproche.workflow.service.WorkflowNotificationService;
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
    private final WorkflowNotificationService notificationService;

    /**
     * Passe l'identifiant de la notification enregistrée avant commit à la phase de remise, qui
     * s'exécute sur le même fil.
     */
    private final ThreadLocal<UUID> notificationsAremettre = new ThreadLocal<>();

    /**
     * Enregistre la notification à remettre, dans la transaction de la transition : les deux sont
     * ainsi committées ensemble, ou pas du tout. Aucun appel réseau ici — seulement une écriture.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void enregistrerNotification(TransitionFranchieEvent event) {
        WorkflowValidationInstance instance = instanceConcernee(event);
        if (instance == null) {
            return;
        }

        Map<String, Object> payload = construireCharge(event, etapeAtteinte(event.getEtatApres()));
        WorkflowNotification notification =
                notificationService.enregistrer(instance.getResourceId(), instance.getResourceType(), payload);
        notificationsAremettre.set(notification.getId());
    }

    /**
     * Tente la remise immédiatement après le commit, pour que le service métier soit à jour sans
     * attendre le prochain passage de l'ordonnanceur. Un échec n'est pas grave : la notification
     * reste en attente et sera rejouée.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void remettreNotification(TransitionFranchieEvent event) {
        UUID notificationId = notificationsAremettre.get();
        notificationsAremettre.remove();

        if (notificationId != null) {
            notificationService.remettre(notificationId);
        }

        etapeAtteinte(event.getEtatApres()).ifPresent(step -> notifierParEmail(step, event));
    }

    private WorkflowValidationInstance instanceConcernee(TransitionFranchieEvent event) {
        if (!INSTANCE_CLASS.equals(event.getEntityClass()) || event.getEtatApres() == null) {
            return null;
        }
        return chargerInstance(event.getEntityId());
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
