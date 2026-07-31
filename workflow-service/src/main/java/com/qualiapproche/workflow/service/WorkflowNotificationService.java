package com.qualiapproche.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowNotification.NotificationStatut;
import com.qualiapproche.workflow.repository.WorkflowNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Remise garantie des notifications de transition aux services métier.
 *
 * <p>Enregistrement dans la transaction de la transition, remise immédiate après commit, puis
 * rejeu avec report exponentiel jusqu'à {@value #TENTATIVES_MAX} tentatives. Une notification qui
 * épuise ses tentatives passe en {@code ABANDONNEE} et doit être reprise manuellement — elle
 * reste visible en base, au lieu de disparaître dans un fichier de journal.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowNotificationService {

    /** Au-delà, la notification cesse d'être rejouée et attend une reprise manuelle. */
    public static final int TENTATIVES_MAX = 8;

    /** Report avant nouvelle tentative : 1 min, puis doublement à chaque échec (plafonné à 1 h). */
    private static final long REPORT_INITIAL_SECONDES = 60;
    private static final long REPORT_MAX_SECONDES = 3600;

    private final WorkflowNotificationRepository notificationRepository;
    private final SupportWebhookClient supportWebhookClient;
    private final AmeliorationWebhookClient ameliorationWebhookClient;
    private final ObjectMapper objectMapper;

    /**
     * Enregistre la notification à remettre. Appelé avant le commit de la transition pour que les
     * deux soient atomiques.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WorkflowNotification enregistrer(String resourceId, String resourceType, Map<String, Object> payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            // Ne doit pas arriver : la charge utile n'est faite que de types simples.
            throw new IllegalStateException("Charge utile de notification non sérialisable", e);
        }

        return notificationRepository.save(WorkflowNotification.builder()
                .resourceId(resourceId)
                .resourceType(resourceType)
                .payload(json)
                .statut(NotificationStatut.A_REMETTRE)
                .build());
    }

    /**
     * Tente la remise d'une notification. Chaque tentative est isolée dans sa propre transaction :
     * un échec ne doit ni annuler la transition déjà committée, ni empêcher les autres remises.
     *
     * @return vrai si la notification a été remise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean remettre(UUID notificationId) {
        WorkflowNotification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || notification.getStatut() != NotificationStatut.A_REMETTRE) {
            return false;
        }

        notification.setTentatives(notification.getTentatives() + 1);
        try {
            appelerServiceMetier(notification);
            notification.setStatut(NotificationStatut.REMISE);
            notification.setRemiseAt(LocalDateTime.now());
            notification.setDerniereErreur(null);
            notificationRepository.save(notification);
            return true;
        } catch (Exception e) {
            marquerEchec(notification, e);
            notificationRepository.save(notification);
            return false;
        }
    }

    private void marquerEchec(WorkflowNotification notification, Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        notification.setDerniereErreur(message.length() > 1000 ? message.substring(0, 1000) : message);

        if (notification.getTentatives() >= TENTATIVES_MAX) {
            notification.setStatut(NotificationStatut.ABANDONNEE);
            log.error("Notification {} abandonnée après {} tentatives : la ressource {} ({}) reste "
                            + "désynchronisée et doit être reprise manuellement. Dernière erreur : {}",
                    notification.getId(), notification.getTentatives(), notification.getResourceId(),
                    notification.getResourceType(), message);
            return;
        }

        long report = Math.min(REPORT_INITIAL_SECONDES * (1L << (notification.getTentatives() - 1)),
                REPORT_MAX_SECONDES);
        notification.setProchaineTentativeAt(LocalDateTime.now().plusSeconds(report));
        log.warn("Échec de remise de la notification {} (tentative {}/{}), nouvelle tentative dans {} s : {}",
                notification.getId(), notification.getTentatives(), TENTATIVES_MAX, report, message);
    }

    private void appelerServiceMetier(WorkflowNotification notification) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(notification.getPayload(), new TypeReference<>() {});
        UUID resourceId = UUID.fromString(notification.getResourceId());
        String resourceType = notification.getResourceType();

        if ("DOCUMENT".equalsIgnoreCase(resourceType)) {
            supportWebhookClient.updateDocumentStatus(resourceId, payload);
            supportWebhookClient.logDocumentAudit(resourceId, auditDepuis(payload));
        } else if ("NON_CONFORMITE".equalsIgnoreCase(resourceType)) {
            ameliorationWebhookClient.updateNonConformiteStatus(resourceId, payload);
        } else if ("PLAN_ACTION".equalsIgnoreCase(resourceType)) {
            ameliorationWebhookClient.updatePlanActionStatus(resourceId, payload);
        } else {
            // Aucun destinataire : inutile de rejouer indéfiniment une notification sans cible.
            throw new IllegalStateException("Aucun service destinataire pour le type de ressource '"
                    + resourceType + "'");
        }
    }

    private Map<String, Object> auditDepuis(Map<String, Object> payload) {
        Object comments = payload.get("comments");
        return Map.of(
                "action", "TRANSITION_" + payload.get("decision"),
                "details", comments != null
                        ? comments
                        : "Passage à l'étape « " + payload.get("statusName") + " »");
    }

    /**
     * Rejoue les notifications en attente dont l'échéance est atteinte. Le lot est borné pour
     * qu'un arriéré important ne monopolise pas l'ordonnanceur.
     */
    @Transactional(readOnly = true)
    public List<UUID> notificationsARejouer(int taille) {
        return notificationRepository
                .findByStatutAndProchaineTentativeAtLessThanEqualOrderByCreatedAtAsc(
                        NotificationStatut.A_REMETTRE, LocalDateTime.now(), PageRequest.of(0, taille))
                .stream()
                .map(WorkflowNotification::getId)
                .toList();
    }
}
