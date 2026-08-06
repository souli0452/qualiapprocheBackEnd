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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "workflow_step_field")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    @JsonIgnore
    private WorkflowStep step;

    @Column(nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType type;

    @Column(nullable = false)
    private boolean isRequired;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON array string for SELECT choices

    /**
     * Décision à laquelle ce champ se rapporte, ou {@code null} s'il vaut pour toutes.
     *
     * <p>Un justificatif de rejet n'a de sens qu'au rejet. Attaché à l'étape sans plus de
     * précision, il s'affichait aussi quand l'utilisateur approuvait : on lui demandait de
     * justifier un refus qu'il n'était pas en train de prononcer.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private StepDecision decision;

    /**
     * Action précise à laquelle ce champ se rapporte, désignée par son code, ou {@code null} s'il
     * vaut pour toutes celles que sa {@link #decision} laisse passer.
     *
     * <p>La portée par décision ne suffit plus dès qu'une étape offre plusieurs actions de même
     * nature : le motif que réclame « Demander un complément » serait exigé de qui valide
     * simplement, les deux approuvant. Le code de l'action est le seul repère qui les
     * distingue.</p>
     *
     * <p>Les deux portées se cumulent, la plus étroite l'emportant : un champ qui nomme une action
     * n'est demandé que par elle.</p>
     */
    @Column(name = "action_code", length = 60)
    private String actionCode;
}
