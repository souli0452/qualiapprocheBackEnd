package com.qualiapproche.repository;

import com.qualiapproche.entities.SuiviAuditInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SuiviAuditInspectionRepository extends JpaRepository<SuiviAuditInspection, UUID> {
}
