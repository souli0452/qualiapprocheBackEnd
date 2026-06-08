package com.qualiapproche.support.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "qms_workflow_step")
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
    private DocumentWorkflow workflow;

    @Column(nullable = false)
    private String nomEtape; // ex: "Validation DRH", "Approbation Direction"

    @Column(nullable = false)
    private int stepOrder; // Ordre d'exécution (0, 1, 2...)

    private String responsableRole; // Rôle requis pour valider cette étape
    private String description;
}
