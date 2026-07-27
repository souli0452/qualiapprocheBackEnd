package com.qualiapproche.support.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    private int stepOrder; // Ordre d'exécution (0, 1, 2...) — sert de point d'entrée et d'ordre d'affichage

    private String responsableRole; // Rôle requis par défaut pour agir sur cette étape
    private String description;

    /**
     * Modèle du catalogue d'étapes réutilisables dont cette étape a été instanciée (copie de
     * nomEtape/responsableRole au moment de la sélection). Purement informatif : sert à
     * pré-sélectionner la bonne entrée du catalogue quand on ré-ouvre le workflow en édition.
     * Nullable — aucune contrainte, aucune cascade.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_template_id")
    private WorkflowStepTemplate stepTemplate;

    /** Transitions sortantes explicites (une par décision possible) — voir {@link WorkflowTransition}. */
    @OneToMany(mappedBy = "fromStep", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowTransition> transitions = new ArrayList<>();
}
