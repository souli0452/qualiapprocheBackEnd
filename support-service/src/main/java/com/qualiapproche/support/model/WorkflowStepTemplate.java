package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Modèle réutilisable d'étape de circuit de validation (catalogue), indépendant de tout
 * workflow précis. En assemblant un {@link DocumentWorkflow}, on choisit une entrée de ce
 * catalogue pour pré-remplir une {@link WorkflowStep} — qui reste ensuite une copie propre à
 * ce workflow (nomEtape/responsableRole dupliqués), sans lien vivant ni cascade.
 */
@Entity
@Table(name = "qms_workflow_step_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkflowStepTemplate extends AuditEntity {

    @Column(nullable = false)
    private String nomEtape;

    @Column(nullable = false)
    private String responsableRole;

    private String description;
}
