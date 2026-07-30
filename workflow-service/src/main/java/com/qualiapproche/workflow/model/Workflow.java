package com.qualiapproche.workflow.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "workflow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Workflow extends AuditEntity {

    @Column(nullable = false)
    private String nom; // ex: Validation Standard, Validation Complète

    private String description;

    @Column(nullable = false)
    private String resourceType; // ex: "DOCUMENT", "NON_CONFORMITE"

    /**
     * Un seul workflow doit être actif par resourceType : c'est celui que les services
     * métier utilisent pour initier une instance. Sans ce marqueur explicite, le premier
     * workflow renvoyé par la base (ordre non garanti) était choisi arbitrairement.
     * {@code columnDefinition} force les lignes existantes à {@code true} lors de la migration.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean actif = true;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    @JsonIgnoreProperties("workflow")
    private List<WorkflowStep> steps = new ArrayList<>();

    public void addStep(WorkflowStep step) {
        steps.add(step);
        step.setWorkflow(this);
    }
}
