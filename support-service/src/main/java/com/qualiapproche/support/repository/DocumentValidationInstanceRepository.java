package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DocumentValidationInstance;
import com.qualiapproche.support.model.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentValidationInstanceRepository extends JpaRepository<DocumentValidationInstance, UUID> {
    Optional<DocumentValidationInstance> findByDocumentId(UUID documentId);

    Optional<DocumentValidationInstance> findByDocumentIdAndStatus(UUID documentId, ValidationStatus status);

    Optional<DocumentValidationInstance> findTopByDocumentIdOrderByStartedAtDesc(UUID documentId);

    boolean existsByCurrentStepId(Long currentStepId);
}
