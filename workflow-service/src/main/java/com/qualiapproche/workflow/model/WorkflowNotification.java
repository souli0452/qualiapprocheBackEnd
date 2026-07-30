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

    public enum NotificationStatut {
        /** En attente de remise ou de nouvelle tentative. */
        A_REMETTRE,
        /** Remise confirmée par le service destinataire. */
        REMISE,
        /** Nombre maximal de tentatives atteint : nécessite une reprise manuelle. */
        ABANDONNEE
    }
}
