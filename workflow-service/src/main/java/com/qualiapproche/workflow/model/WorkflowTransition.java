package com.qualiapproche.workflow.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "workflow_transition",
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
    @JsonIgnore
    private WorkflowStep fromStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepDecision decision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_step_id")
    @JsonIgnoreProperties({"transitions", "fields", "workflow"})
    private WorkflowStep toStep;

    private String requiredRole;
    private String label;

    /**
     * La transition clôt le circuit au lieu de mener à une autre étape.
     *
     * <p>Marqueur explicite, et non déduit de l'absence de destination : une transition sans
     * destination pouvait aussi bien signifier « cette décision termine le dossier » que
     * « cette décision n'a pas lieu d'être ici ». Le moteur proposait donc comme action des
     * transitions que personne n'avait voulues.</p>
     */
    @Column(name = "terminal", nullable = false)
    @Builder.Default
    private boolean terminal = false;
}
