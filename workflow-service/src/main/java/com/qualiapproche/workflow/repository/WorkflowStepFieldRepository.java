package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowStepField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowStepFieldRepository extends JpaRepository<WorkflowStepField, Long> {

    /**
     * Champs dont la saisie conditionne le franchissement d'une étape.
     * L'attribut porte le nom du champ Java {@code isRequired}, et non {@code required}
     * exposé par l'accesseur : c'est ce nom que la dérivation de requête attend.
     */
    java.util.List<WorkflowStepField> findByStepIdAndIsRequiredTrue(Long stepId);
}
