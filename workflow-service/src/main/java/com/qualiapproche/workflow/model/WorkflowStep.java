package com.qualiapproche.workflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "workflow_step")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnoreProperties("steps")
    private Workflow workflow;

    @Column(nullable = false)
    private String nomEtape;

    @Column(nullable = false)
    private int stepOrder;

    private String responsableRole;
    private String description;

    @Column(name = "etat_traitement")
    private String etatTraitement;

    @Column(name = "email_template_code")
    private String emailTemplateCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_template_id")
    private WorkflowStepTemplate stepTemplate;

    @OneToMany(mappedBy = "fromStep", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("fromStep")
    private List<WorkflowTransition> transitions = new ArrayList<>();

    @OneToMany(mappedBy = "step", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("step")
    private List<WorkflowStepField> fields = new ArrayList<>();
}
