package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.model.StepDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    Optional<WorkflowTransition> findByFromStepIdAndDecision(Long fromStepId, StepDecision decision);
}
