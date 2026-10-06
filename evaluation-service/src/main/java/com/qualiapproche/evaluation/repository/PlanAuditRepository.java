package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.PlanAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanAuditRepository extends JpaRepository<PlanAudit, UUID> {

    Optional<PlanAudit> findByAuditId(UUID auditId);
}
