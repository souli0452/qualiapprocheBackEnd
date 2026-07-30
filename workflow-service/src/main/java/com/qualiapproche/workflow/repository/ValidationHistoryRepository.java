package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.ValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ValidationHistoryRepository extends JpaRepository<ValidationHistory, Long> {
    java.util.Optional<ValidationHistory> findTopByValidationInstanceOrderByDecisionDateDesc(com.qualiapproche.workflow.model.WorkflowValidationInstance validationInstance);

    /** Traçabilité complète d'une instance, de la plus ancienne décision à la plus récente. */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "fieldValues")
    java.util.List<ValidationHistory> findByValidationInstance_IdOrderByDecisionDateAsc(java.util.UUID instanceId);
}
