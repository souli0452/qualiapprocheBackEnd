package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.AffectationPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AffectationPointRepository extends JpaRepository<AffectationPoint, UUID> {

    List<AffectationPoint> findAllByAuditId(UUID auditId);

    void deleteAllByAuditId(UUID auditId);
}
