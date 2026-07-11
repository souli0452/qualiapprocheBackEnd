package com.qualiapproche.support.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "qms_validation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_instance_id", nullable = false)
    private DocumentValidationInstance validationInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private WorkflowStep step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepDecision decision; // APPROUVE, REJETE

    private String comments;

    @Column(nullable = false)
    private String validatorUserId;

    @Builder.Default
    private LocalDateTime decisionDate = LocalDateTime.now();

    private String documentHash;
}

