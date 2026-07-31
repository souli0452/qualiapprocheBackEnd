package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Modèle réutilisable d'étape de circuit de validation (catalogue), indépendant de tout
 * workflow précis. En assemblant un {@link DocumentWorkflow}, on choisit une entrée de ce
 * catalogue pour pré-remplir une {@link WorkflowStep} — qui reste ensuite une copie propre à
 * ce workflow (nomEtape/responsableRole dupliqués), sans lien vivant ni cascade.
 */
@Entity
@Table(name = "qms_workflow_step_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_step_template_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkflowStepTemplate extends AuditEntity {

    /**
     * Code fonctionnel de l'entrée de catalogue, unique et immuable.
     *
     * <p>C'est ici que naît le code d'une étape : choisir cette entrée en assemblant un circuit
     * pré-remplit le code de l'étape, de sorte qu'une même nature d'étape porte partout le même
     * identifiant — « Vérification » vaut {@code VERIFICATION} dans tous les circuits, ce qui rend
     * les circuits comparables et les statistiques agrégeables.</p>
     *
     * <p>Le code de l'étape reste néanmoins propre au circuit : une même entrée de catalogue peut
     * être employée deux fois dans un circuit, le second usage étant alors suffixé. Le catalogue
     * fournit la valeur par défaut, il ne porte pas l'identité de l'étape.</p>
     */
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String nomEtape;

    @Column(nullable = false)
    private String responsableRole;

    private String description;
}
