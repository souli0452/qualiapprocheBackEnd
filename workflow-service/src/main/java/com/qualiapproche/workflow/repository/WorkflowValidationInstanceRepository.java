package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.model.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface WorkflowValidationInstanceRepository extends JpaRepository<WorkflowValidationInstance, UUID> {
    Optional<WorkflowValidationInstance> findTopByResourceIdAndStatusOrderByStartedAtDesc(String resourceId, ValidationStatus status);
    Optional<WorkflowValidationInstance> findTopByResourceIdOrderByStartedAtDesc(String resourceId);
    boolean existsByEtatCodeInAndStatus(List<String> etatCodes, ValidationStatus status);
}
