package com.qualiapproche.support.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Transition explicite du circuit de validation : depuis une étape, pour une décision donnée
 * (APPROUVE / REJETE), quelle est l'étape cible ?
 *
 * {@code toStep == null} signifie fin de circuit :
 * - decision == APPROUVE  -> le document est définitivement validé
 * - decision == REJETE    -> le document repasse en brouillon
 *
 * Le routage ne dépend plus de l'ordre (stepOrder) des étapes au moment de la décision :
 * il est figé explicitement ici, ce qui permet le branchement (ex. rejet qui revient
 * directement au rédacteur plutôt qu'à l'étape juste précédente) et reste stable même si
 * le circuit est réordonné après coup.
 */
@Entity
@Table(name = "qms_workflow_transition",
        uniqueConstraints = @UniqueConstraint(name = "uk_workflow_transition_from_decision",
                columnNames = {"from_step_id", "decision"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_step_id", nullable = false)
    private WorkflowStep fromStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepDecision decision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_step_id")
    private WorkflowStep toStep;

    /** Rôle requis pour déclencher CETTE transition précise ; si absent, retombe sur le rôle de l'étape source. */
    private String requiredRole;

    /** Libellé affichable pour l'action (ex. "Rejeter - retour au rédacteur"). */
    private String label;
}