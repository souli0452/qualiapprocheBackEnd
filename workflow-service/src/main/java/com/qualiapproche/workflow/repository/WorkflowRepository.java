package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByResourceType(String resourceType);
    List<Workflow> findByResourceTypeAndActifTrue(String resourceType);
}
