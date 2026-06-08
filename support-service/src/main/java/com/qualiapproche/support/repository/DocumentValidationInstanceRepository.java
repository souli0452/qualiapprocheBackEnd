package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DocumentValidationInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentValidationInstanceRepository extends JpaRepository<DocumentValidationInstance, UUID> {
    Optional<DocumentValidationInstance> findByDocumentId(UUID documentId);
}
