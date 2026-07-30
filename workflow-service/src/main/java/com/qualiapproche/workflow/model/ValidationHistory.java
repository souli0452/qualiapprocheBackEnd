package com.qualiapproche.workflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_validation_history")
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
    private WorkflowValidationInstance validationInstance;

    @Column(name = "step_code", nullable = false)
    private String stepCode;

    @Column(name = "step_name")
    private String stepName;

    @Column(nullable = false)
    private String decision;

    private String comments;

    @Column(nullable = false)
    private String validatorUserId;

    @Builder.Default
    private LocalDateTime decisionDate = LocalDateTime.now();

    private String documentHash;

    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowFieldValue> fieldValues = new ArrayList<>();
}
