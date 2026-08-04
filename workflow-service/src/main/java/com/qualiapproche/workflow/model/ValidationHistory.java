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

    /**
     * Nom de l'auteur de la décision, tel qu'il se présentait au moment où elle a été prise.
     *
     * <p>Seul l'identifiant technique était conservé, et c'est lui que l'écran de traçabilité
     * affichait : la question « qui a validé » n'obtenait pour réponse qu'un UUID. Le résoudre à la
     * lecture aurait supposé un appel à user-service par ligne, et aurait fait mentir la trace le
     * jour où la personne quitte l'organisation ou change de nom — un enregistrement d'audit doit
     * dire ce qui était vrai à sa date, pas ce qui l'est aujourd'hui.</p>
     *
     * <p>Nul sur les décisions antérieures à ce champ : l'identifiant reste alors le seul repère.</p>
     */
    private String validatorFullName;

    @Builder.Default
    private LocalDateTime decisionDate = LocalDateTime.now();

    private String documentHash;

    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowFieldValue> fieldValues = new ArrayList<>();
}
