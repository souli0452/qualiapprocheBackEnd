package com.qualiapproche.workflow.event;

import com.qualiapproche.workflow.model.WorkflowFieldValue;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
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
import java.util.stream.Collectors;

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
 *   <li>{@code status} — valeur machine : {@code EN_COURS}, {@code APPROVED}, {@code REJECTED}
 *       ou {@code CLOSED} ;</li>
 *   <li>{@code statusName} — étape atteinte, lisible par un humain (ou la décision si terminal) ;</li>
 *   <li>{@code etatCode} — état de traitement métier de l'étape ({@code VALIDATION_RS}, {@code CLOTURE}…) ;</li>
 *   <li>{@code comments} — commentaire saisi lors de la décision ;</li>
 *   <li>{@code decision} — code de la transition franchie ;</li>
 *   <li>{@code fields} — valeurs saisies lors de la décision, indexées par nom de champ ;</li>
 *   <li>{@code timestamp} — horodatage de la transition.</li>
 * </ul>
 *
 * <p>{@code fields} est ce qui permet à un module métier de recopier une saisie d'étape dans sa
 * propre donnée — un document justificatif de rejet, par exemple. Sans lui, la saisie restait
 * enfermée dans l'historique du moteur et le module devait la redemander à l'utilisateur.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventListener {

    private static final String INSTANCE_CLASS = WorkflowValidationInstance.class.getName();
    private static final String PREFIXE_ETAT_TERMINAL = "TERMINATED_";

    private final WorkflowStepRepository stepRepository;
    private final WorkflowValidationInstanceRepository validationInstanceRepository;
    private final ValidationHistoryRepository historyRepository;
    private final WorkflowFieldValueRepository fieldValueRepository;
    private final WorkflowNotificationService notificationService;
    private final NotificateurEtapeParEmail notificateurParEmail;

    /**
     * Enregistre la notification à remettre, dans la transaction de la transition : les deux sont
     * ainsi committées ensemble, ou pas du tout. Aucun appel réseau ici — seulement une écriture.
     *
     * <p>L'identifiant obtenu est déposé sur l'événement lui-même, que la phase suivante recevra
     * à l'identique.</p>
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void enregistrerNotification(TransitionFranchieEvent event) {
        WorkflowValidationInstance instance = instanceConcernee(event);
        if (instance == null) {
            return;
        }

        Map<String, Object> payload = construireCharge(event, etapeAtteinte(event.getEtatApres()), instance);
        WorkflowNotification notification =
                notificationService.enregistrer(instance.getResourceId(), instance.getResourceType(), payload);
        event.setNotificationId(notification.getId());
        // La phase suivante compose le courriel : elle a besoin de la ressource, pas de l'instance.
        event.setResourceId(instance.getResourceId());
        event.setResourceType(instance.getResourceType());
    }

    /**
     * Tente la remise immédiatement après le commit, pour que le service métier soit à jour sans
     * attendre le prochain passage de l'ordonnanceur. Un échec n'est pas grave : la notification
     * reste en attente et sera rejouée.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void remettreNotification(TransitionFranchieEvent event) {
        UUID notificationId = event.getNotificationId();
        if (notificationId != null) {
            notificationService.remettre(notificationId);
        }

        etapeAtteinte(event.getEtatApres()).ifPresent(step -> notificateurParEmail.notifier(step, event));
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
        // Le contrôle de nullité couvre la phase d'après commit, qui n'est pas précédée du
        // filtrage opéré par instanceConcernee().
        if (etatApres == null || estTerminal(etatApres)) {
            return Optional.empty();
        }
        try {
            return stepRepository.findById(Long.valueOf(etatApres));
        } catch (NumberFormatException e) {
            log.warn("L'état atteint '{}' n'est ni terminal ni un identifiant d'étape.", etatApres);
            return Optional.empty();
        }
    }

    /**
     * Compose la charge utile à partir de l'état atteint.
     *
     * <p>Le caractère terminal se lit sur le <b>code de l'état</b>, et non sur l'absence d'étape.
     * Les deux étaient confondus : {@link #etapeAtteinte} rend également un résultat vide quand
     * l'étape est simplement introuvable — supprimée du circuit, ou code non numérique. La charge
     * était alors construite comme celle d'une fin de circuit, et le retrait du préfixe
     * {@code TERMINATED_} s'appliquait à un code qui ne le portait pas : l'exception qui en
     * résultait remontait depuis un écouteur d'avant commit et <b>annulait la transition</b>,
     * transformant une étape mal configurée en échec de la décision de l'utilisateur.</p>
     */
    private Map<String, Object> construireCharge(TransitionFranchieEvent event, Optional<WorkflowStep> etapeAtteinte,
                                                 WorkflowValidationInstance instance) {
        Map<String, Object> payload = new HashMap<>();
        String etatApres = event.getEtatApres();

        if (estTerminal(etatApres)) {
            // Etat terminal : le suffixe porte la décision finale (APPROUVE / REJETE / CLOTURE).
            String decision = etatApres.substring(PREFIXE_ETAT_TERMINAL.length());
            payload.put("status", issueFinale(decision));
            payload.put("statusName", decision);
            payload.put("etatCode", null);
        } else if (etapeAtteinte.isPresent()) {
            WorkflowStep step = etapeAtteinte.get();
            payload.put("status", "EN_COURS");
            payload.put("statusName", step.getNomEtape());
            payload.put("etatCode", step.getEtatTraitement());
        } else {
            // Étape introuvable sur un état non terminal : le circuit reste en cours. Le service
            // métier est informé avec ce dont on dispose, plutôt que de le laisser sans nouvelle.
            log.warn("L'étape « {} » atteinte par la ressource est introuvable : la notification "
                    + "est émise sans libellé d'étape.", etatApres);
            payload.put("status", "EN_COURS");
            payload.put("statusName", etatApres);
            payload.put("etatCode", null);
        }

        payload.put("comments", event.getCommentaire());
        payload.put("decision", event.getTransitionCode());
        payload.put("fields", champsSaisis(instance));
        payload.put("timestamp", LocalDateTime.now().toString());
        return payload;
    }

    /**
     * Valeurs saisies lors de la décision qui vient d'être prise, indexées par nom de champ.
     *
     * <p>Elles sont relues depuis l'historique plutôt que portées par l'événement : celui-ci est
     * publié au franchissement, alors que les valeurs sont rattachées juste après. Cette phase
     * d'avant commit voit donc l'ensemble, là où l'événement ne verrait rien.</p>
     *
     * <p>Le nom du champ sert de clé — c'est le seul repère stable pour le module métier, les
     * identifiants d'étapes changeant d'une installation à l'autre.</p>
     */
    private Map<String, String> champsSaisis(WorkflowValidationInstance instance) {
        return historyRepository.findTopByValidationInstanceOrderByDecisionDateDesc(instance)
                .map(history -> fieldValueRepository.findByHistory_Id(history.getId()).stream()
                        .filter(valeur -> valeur.getFieldName() != null && valeur.getValue() != null)
                        .collect(Collectors.toMap(
                                WorkflowFieldValue::getFieldName,
                                WorkflowFieldValue::getValue,
                                (premiere, seconde) -> seconde)))
                .orElseGet(Map::of);
    }

    private boolean estTerminal(String etatApres) {
        return etatApres != null && etatApres.startsWith(PREFIXE_ETAT_TERMINAL);
    }

    /**
     * Issue publiée quand le circuit se termine, déduite de la nature de la décision finale.
     *
     * <p>Le rejet servait de valeur par défaut : tout suffixe qui n'était pas {@code APPROUVE}
     * devenait {@code REJECTED}. L'arrivée de la clôture rend cette économie fausse — un dossier
     * mis en clôture n'est pas un dossier rejeté — et chaque nature nomme donc son issue.</p>
     */
    private String issueFinale(String decision) {
        if ("APPROUVE".equalsIgnoreCase(decision)) {
            return "APPROVED";
        }
        if ("CLOTURE".equalsIgnoreCase(decision)) {
            return "CLOSED";
        }
        return "REJECTED";
    }

}
