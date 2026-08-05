package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.model.StepDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    Optional<WorkflowTransition> findByFromStepIdAndDecision(Long fromStepId, StepDecision decision);

    /** Faits déjà exigés par au moins une transition, tous circuits confondus. */
    @Query("select distinct t.conditionRequise from WorkflowTransition t where t.conditionRequise is not null")
    List<String> faitsExiges();
}
