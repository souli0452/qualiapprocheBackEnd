package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.SuiviAuditInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SuiviAuditInspectionRepository extends JpaRepository<SuiviAuditInspection, UUID> {
}
