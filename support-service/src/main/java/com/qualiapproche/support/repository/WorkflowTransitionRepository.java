package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.StepDecision;
import com.qualiapproche.support.model.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    Optional<WorkflowTransition> findByFromStepIdAndDecision(Long fromStepId, StepDecision decision);

    List<WorkflowTransition> findByToStepId(Long toStepId);
}