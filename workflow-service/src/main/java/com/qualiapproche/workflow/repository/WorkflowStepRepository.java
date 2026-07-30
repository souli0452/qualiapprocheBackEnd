package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    Optional<WorkflowStep> findByNomEtapeAndWorkflow_Nom(String nomEtape, String workflowNom);
    Optional<WorkflowStep> findByNomEtapeAndWorkflow_Id(String nomEtape, java.util.UUID workflowId);
}
