package com.qualiapproche.workflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification à remettre à un service métier après une transition (patron « outbox »).
 *
 * <p>La ligne est écrite dans la <b>même transaction</b> que la transition : soit les deux sont
 * enregistrées, soit aucune. La remise est ensuite tentée immédiatement après le commit, puis
 * rejouée par {@code WorkflowNotificationScheduler} tant qu'elle n'a pas abouti. Avant cela, une
 * indisponibilité du destinataire au moment précis de la transition laissait définitivement le
 * statut métier désynchronisé, sans autre trace qu'une ligne de journal.</p>
 */
@Entity
@Table(name = "workflow_notification",
        indexes = @Index(name = "idx_workflow_notification_statut", columnList = "statut, prochaine_tentative_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private String resourceType;

    /**
     * Nature de la remise : appel au service métier, ou courriel à un responsable d'étape.
     *
     * <p>Les courriels partaient jusqu'ici « au mieux » : un serveur SMTP indisponible, ou un
     * mot de passe expiré, et la notification était perdue sans autre trace qu'une ligne de
     * journal — le responsable d'étape n'apprenait jamais qu'il avait un dossier à traiter. Les
     * faire passer par le même registre leur donne les mêmes reprises, le même report exponentiel
     * et le même abandon explicite après épuisement des tentatives.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CanalRemise canal = CanalRemise.WEBHOOK;

    /** Charge utile sérialisée en JSON, telle qu'elle sera postée au service destinataire. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatut statut;

    @Column(name = "tentatives", nullable = false)
    @Builder.Default
    private int tentatives = 0;

    @Column(name = "derniere_erreur", length = 1000)
    private String derniereErreur;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Date à partir de laquelle une nouvelle tentative est autorisée (report exponentiel). */
    @Column(name = "prochaine_tentative_at")
    @Builder.Default
    private LocalDateTime prochaineTentativeAt = LocalDateTime.now();

    @Column(name = "remise_at")
    private LocalDateTime remiseAt;

    public enum CanalRemise {
        /** Appel au service métier propriétaire de la ressource. */
        WEBHOOK,
        /** Courriel à un responsable d'étape. */
        EMAIL
    }

    public enum NotificationStatut {
        /** En attente de remise ou de nouvelle tentative. */
        A_REMETTRE,
        /**
         * Revendiquée par un ouvrier, remise en cours.
         *
         * <p>Marque la notification comme prise en charge, pour qu'aucun autre ouvrier — autre
         * pod, ou ordonnanceur concurrent de la remise immédiate d'après commit — ne la poste
         * une seconde fois. La revendication porte une échéance dans
         * {@code prochaineTentativeAt} : si l'ouvrier disparaît en cours de route, la
         * notification redevient reprenable passé ce délai plutôt que de rester bloquée.</p>
         */
        EN_COURS_DE_REMISE,
        /** Remise confirmée par le service destinataire. */
        REMISE,
        /** Nombre maximal de tentatives atteint : nécessite une reprise manuelle. */
        ABANDONNEE
    }
}
