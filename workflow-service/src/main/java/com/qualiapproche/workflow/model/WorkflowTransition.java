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
}
