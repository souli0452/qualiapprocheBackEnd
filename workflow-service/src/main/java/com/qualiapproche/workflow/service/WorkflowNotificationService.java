package com.qualiapproche.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowNotification.NotificationStatut;
import com.qualiapproche.workflow.repository.WorkflowNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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


 *
 * <p>Chaque remise est précédée d'une <b>revendication</b> : la notification est marquée comme
 * prise en charge avant tout appel réseau, en une écriture conditionnelle que la base arbitre.
 * C'est ce qui rend la remise unique — sans elle, l'ordonnanceur et la remise immédiate d'après
 * commit, ou simplement deux pods, pouvaient poster la même notification chacun de leur côté, et
 * le service destinataire enregistrait deux fois la décision.</p>
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

    /**
     * Durée au-delà de laquelle une revendication sans suite est considérée comme abandonnée.
     *
     * <p>Doit rester confortablement supérieure au temps d'un appel webhook, délai d'expiration
     * compris : trop courte, elle ferait reprendre une remise encore en cours.</p>
     */
    private static final long REPRISE_APRES_REVENDICATION_SECONDES = 300;

    /** Statuts depuis lesquels une notification peut être (re)prise en charge. */
    private static final List<NotificationStatut> STATUTS_REPRENABLES =
            List.of(NotificationStatut.A_REMETTRE, NotificationStatut.EN_COURS_DE_REMISE);

    private final WorkflowNotificationRepository notificationRepository;
    private final SupportWebhookClient supportWebhookClient;
    private final SmtpEmailService emailService;
    private final AmeliorationWebhookClient ameliorationWebhookClient;
    private final ObjectMapper objectMapper;

    /**
     * Référence à soi-même <b>à travers le proxy</b>, pour que la revendication et la remise
     * soient bien deux transactions distinctes.
     *
     * <p>Un appel direct de méthode à méthode passerait à côté du proxy transactionnel de Spring
     * et les fondrait en une seule transaction : le verrou de ligne posé par la revendication
     * serait alors conservé pendant tout l'appel réseau, bloquant les autres ouvriers au lieu de
     * les écarter aussitôt.</p>
     *
     * <p>Injecté sur le champ et non par le constructeur : {@code @Lazy} — qui rompt le cycle de
     * construction d'un bean sur lui-même — n'est pas recopié par Lombok sur le paramètre du
     * constructeur qu'il génère, et l'injection échouerait au démarrage.</p>
     */
    @Autowired
    @Lazy
    private WorkflowNotificationService self;

    /**
     * Enregistre la notification à remettre. Appelé avant le commit de la transition pour que les
     * deux soient atomiques.
     */
    /**
     * Enregistre un courriel à remettre, avec les mêmes garanties que les appels métier.
     *
     * <p>Contrairement à ceux-ci, il n'est pas écrit dans la transaction de la transition : les
     * destinataires sont résolus auprès de user-service, et tenir la transaction ouverte pendant
     * cet appel réseau la rendrait aussi longue que le service interrogé est lent. La remise est
     * donc enregistrée après le commit, puis tentée aussitôt — et rejouée par l'ordonnanceur si
     * elle échoue.</p>
     */
    @Transactional
    public WorkflowNotification enregistrerCourriel(String resourceId, String resourceType,
                                                    String destinataire, String sujet, String corps,
                                                    Map<String, String> variables) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("destinataire", destinataire);
        payload.put("sujet", sujet);
        payload.put("corps", corps);
        payload.put("variables", variables);
        return enregistrerAvecCanal(resourceId, resourceType, payload,
                WorkflowNotification.CanalRemise.EMAIL);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public WorkflowNotification enregistrer(String resourceId, String resourceType, Map<String, Object> payload) {
        return enregistrerAvecCanal(resourceId, resourceType, payload,
                WorkflowNotification.CanalRemise.WEBHOOK);
    }

    private WorkflowNotification enregistrerAvecCanal(String resourceId, String resourceType,
                                                      Map<String, Object> payload,
                                                      WorkflowNotification.CanalRemise canal) {
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
                .canal(canal)
                .statut(NotificationStatut.A_REMETTRE)
                .build());
    }

    /**
     * Remet une notification, après l'avoir revendiquée.
     *
     * <p>Les deux étapes sont deux transactions distinctes, et c'est délibéré : la revendication
     * doit être committée — donc visible des autres ouvriers — <b>avant</b> que l'appel réseau ne
     * commence. Menées dans une même transaction, le verrou de ligne serait tenu pendant tout
     * l'appel et les concurrents attendraient au lieu de passer leur chemin.</p>
     *
     * @return vrai si la notification a été remise par cet appel
     */
    public boolean remettre(UUID notificationId) {
        if (!self.revendiquer(notificationId)) {
            return false;
        }
        return self.livrer(notificationId);
    }

    /**
     * Marque la notification comme prise en charge par cet ouvrier.
     *
     * @return vrai si la revendication est acquise ; faux si un autre ouvrier l'a précédée, si la
     *         notification a déjà été remise, ou si son échéance n'est pas atteinte
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean revendiquer(UUID notificationId) {
        LocalDateTime aMaintenant = LocalDateTime.now();
        return notificationRepository.revendiquer(
                notificationId,
                NotificationStatut.EN_COURS_DE_REMISE,
                STATUTS_REPRENABLES,
                aMaintenant,
                aMaintenant.plusSeconds(REPRISE_APRES_REVENDICATION_SECONDES)) == 1;
    }

    /**
     * Poste la notification revendiquée au service destinataire et fige son issue. Isolée dans sa
     * propre transaction : un échec ne doit ni annuler la transition déjà committée, ni empêcher
     * les autres remises.
     *
     * @return vrai si la notification a été remise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean livrer(UUID notificationId) {
        WorkflowNotification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return false;
        }

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

    /**
     * Enregistre l'échec d'une remise et programme la suivante.
     *
     * <p>Le compteur de tentatives a déjà été incrémenté par la revendication : il n'est pas
     * touché ici. La notification est en revanche rendue au statut « à remettre », faute de quoi
     * elle resterait marquée comme prise en charge et ne serait reprise qu'à l'expiration de la
     * revendication.</p>
     */
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
        notification.setStatut(NotificationStatut.A_REMETTRE);
        notification.setProchaineTentativeAt(LocalDateTime.now().plusSeconds(report));
        log.warn("Échec de remise de la notification {} (tentative {}/{}), nouvelle tentative dans {} s : {}",
                notification.getId(), notification.getTentatives(), TENTATIVES_MAX, report, message);
    }

    private void appelerServiceMetier(WorkflowNotification notification) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(notification.getPayload(), new TypeReference<>() {});

        if (notification.getCanal() == WorkflowNotification.CanalRemise.EMAIL) {
            remettreCourriel(payload);
            return;
        }

        UUID resourceId = UUID.fromString(notification.getResourceId());
        String resourceType = notification.getResourceType();

        if ("DOCUMENT".equalsIgnoreCase(resourceType)) {
            supportWebhookClient.updateDocumentStatus(resourceId, payload);
            supportWebhookClient.logDocumentAudit(resourceId, auditDepuis(payload));
        } else if ("DEMANDE_DOCUMENT".equalsIgnoreCase(resourceType)) {
            // La demande a son propre point de remise : son aboutissement ne se réduit pas à un
            // changement d'état — accepter une modification ouvre le dépôt du remplaçant, accepter
            // une suppression retire le document.
            supportWebhookClient.updateDemandeStatus(resourceId, payload);
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

    /**
     * Remet un courriel. Toute exception laisse la notification reprenable : c'est ce qui distingue
     * cette remise de l'ancien envoi, où l'échec se réduisait à une ligne de journal.
     */
    @SuppressWarnings("unchecked")
    private void remettreCourriel(Map<String, Object> payload) {
        String destinataire = (String) payload.get("destinataire");
        String sujet = (String) payload.get("sujet");
        String corps = (String) payload.get("corps");
        Object variables = payload.get("variables");

        emailService.sendEmail(destinataire, sujet, corps,
                variables instanceof Map<?, ?> map ? (Map<String, String>) map : Map.of());
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
     * Notifications à reprendre : celles en attente dont l'échéance est atteinte, et celles
     * dont la revendication a expiré sans que la remise aboutisse — un pod arrêté en cours de
     * route, typiquement. Sans ce second cas, une notification revendiquée puis abandonnée
     * resterait indéfiniment en suspens.
     *
     * <p>Le lot est borné pour qu'un arriéré important ne monopolise pas l'ordonnanceur.</p>
     */
    @Transactional(readOnly = true)
    public List<UUID> notificationsARejouer(int taille) {
        return notificationRepository
                .aReprendre(STATUTS_REPRENABLES, LocalDateTime.now(), PageRequest.of(0, taille))
                .stream()
                .map(WorkflowNotification::getId)
                .toList();
    }
}
