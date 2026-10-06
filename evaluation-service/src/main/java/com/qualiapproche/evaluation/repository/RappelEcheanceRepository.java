package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.RappelEcheance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RappelEcheanceRepository extends JpaRepository<RappelEcheance, UUID> {

    boolean existsByAuditIdAndRegleIdAndDateRappel(UUID auditId, UUID regleId, LocalDate dateRappel);

    List<RappelEcheance> findTop10ByDirectionIdOrderByCreatedAtDesc(UUID directionId);
}
