package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.ValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ValidationHistoryRepository extends JpaRepository<ValidationHistory, Long> {
    java.util.Optional<ValidationHistory> findTopByValidationInstanceOrderByDecisionDateDesc(com.qualiapproche.workflow.model.WorkflowValidationInstance validationInstance);
}
