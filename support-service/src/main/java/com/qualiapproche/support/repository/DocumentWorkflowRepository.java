package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DocumentWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentWorkflowRepository extends JpaRepository<DocumentWorkflow, UUID> {
}
