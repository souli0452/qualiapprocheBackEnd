package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowStepField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowStepFieldRepository extends JpaRepository<WorkflowStepField, Long> {
}
