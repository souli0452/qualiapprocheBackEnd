package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowFieldValueRepository extends JpaRepository<WorkflowFieldValue, Long> {
}
