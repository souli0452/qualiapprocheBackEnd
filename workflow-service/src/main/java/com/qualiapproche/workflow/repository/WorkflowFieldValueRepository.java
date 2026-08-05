package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowFieldValueRepository extends JpaRepository<WorkflowFieldValue, Long> {

    /**
     * Valeurs saisies lors d'une décision.
     *
     * <p>Interrogées par identifiant d'historique plutôt qu'à travers la collection de
     * {@code ValidationHistory} : les valeurs sont enregistrées une à une après le franchissement,
     * sans être ajoutées à la collection en mémoire de l'historique déjà chargé.</p>
     */
    java.util.List<WorkflowFieldValue> findByHistory_Id(Long historyId);
}
