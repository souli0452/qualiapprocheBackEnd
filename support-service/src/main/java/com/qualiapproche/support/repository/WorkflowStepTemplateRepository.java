package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.WorkflowStepTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkflowStepTemplateRepository extends JpaRepository<WorkflowStepTemplate, UUID> {
}
