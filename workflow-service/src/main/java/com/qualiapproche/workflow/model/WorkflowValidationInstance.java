package com.qualiapproche.workflow.model;

import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_validation_instance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowValidationInstance implements IWorkflowData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String resourceId; // UUID or ID as String

    @Column(nullable = false)
    private String resourceType; // e.g. "DOCUMENT", "NON_CONFORMITE"

    @Column(name = "workflow_code", nullable = false)
    private String workflowCode;

    @Column(name = "etat_code", nullable = false)
    private String etatCode;

    @Column(name = "observation", length = 2000)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus status;

    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Transient
    private Etat etatCourant;

    @Override
    public Etat getEtat() {
        return this.etatCourant;
    }

    @Override
    public void setEtat(Etat pEtat) {
        this.etatCourant = pEtat;
    }

    @Override
    public void appliquerEtat(Etat pEtat) {
        this.setEtat(pEtat);
        this.setEtatCode(pEtat != null ? pEtat.getCode() : null);
    }
}
