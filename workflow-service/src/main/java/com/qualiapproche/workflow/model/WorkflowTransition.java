package com.qualiapproche.workflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    /** Libellé du bouton d'action. À défaut, le nom de la décision est repris. */
    private String label;

    /**
     * Icône du bouton d'action, exprimée en classe PrimeIcons ({@code "pi pi-check"}).
     *
     * <p>Stockée telle qu'elle est saisie : la liste des icônes appartient au thème du client, et
     * la figer côté serveur obligerait à livrer le back à chaque nouvelle icône.</p>
     */
    @Column(name = "icon")
    private String icon;

    /** Couleur du bouton d'action. À défaut, celle que porte la décision. */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private SeveriteAction severity;

    /**
     * Fait qui doit être établi sur le dossier pour que la transition soit franchissable.
     *
     * <p>Une décision ne dépend pas que de qui la prend : clore une non-conformité n'a de sens que
     * si ses plans d'action sont soldés. Le moteur ne connaît pas les plans d'action — il ne connaît
     * que des <b>faits</b>, chaînes que le module métier inscrit sur l'instance quand la condition
     * devient vraie. L'un déclare, l'autre exige, et aucun n'a besoin de connaître l'autre.</p>
     *
     * <p>Nul : la transition ne dépend d'aucune condition métier.</p>
     */
    @Column(name = "condition_requise")
    private String conditionRequise;

    /**
     * Ce que la condition veut dire, en clair.
     *
     * <p>Le nom du fait est technique — {@code PLANS_ACTION_SOLDES} — et l'écran ne peut pas le
     * traduire sans se doter d'une table de correspondance en parallèle, qui mentirait dès qu'un
     * circuit déclarerait un fait nouveau. C'est donc l'auteur du circuit qui écrit la phrase, là
     * où il pose la condition.</p>
     */
    @Column(length = 500)
    private String conditionLibelle;

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
