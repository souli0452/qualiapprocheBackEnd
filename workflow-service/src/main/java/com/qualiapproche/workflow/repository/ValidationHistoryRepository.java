package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.ValidationHistory;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface ValidationHistoryRepository extends JpaRepository<ValidationHistory, Long> {
    java.util.Optional<ValidationHistory> findTopByValidationInstanceOrderByDecisionDateDesc(
            WorkflowValidationInstance validationInstance);

    /** Traçabilité complète d'une instance, de la plus ancienne décision à la plus récente. */
    @EntityGraph(attributePaths = "fieldValues")
    java.util.List<ValidationHistory> findByValidationInstance_IdOrderByDecisionDateAsc(java.util.UUID instanceId);

    /**
     * Parmi les instances citées, celles sur lesquelles au moins une décision a été prise.
     *
     * <p>Distingue un circuit ouvert d'un circuit engagé : l'instance est créée avec le dossier,
     * et son existence ne dit donc pas que quelqu'un l'a soumis. Rendue en une requête — la poser
     * dossier par dossier aurait coûté une requête par ligne de tableau de bord.</p>
     */
    @org.springframework.data.jpa.repository.Query(
            "select distinct h.validationInstance.id from ValidationHistory h "
                    + "where h.validationInstance.id in :instanceIds")
    java.util.List<java.util.UUID> instancesAyantUneDecision(
            @org.springframework.data.repository.query.Param("instanceIds")
            java.util.Collection<java.util.UUID> instanceIds);
}
