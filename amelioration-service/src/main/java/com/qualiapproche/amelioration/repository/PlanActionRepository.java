package com.qualiapproche.amelioration.repository;

import com.qualiapproche.common.utils.StatutEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.amelioration.entities.PlanAction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanActionRepository extends JpaRepository<PlanAction, UUID> {
    List<PlanAction> findPlanActionsByResponsableEmailAndStatus(String responsableEmail, StatutEnum statut);
    List<PlanAction> findPlanActionsByResponsableEmail(String responsableEmail);
    List<PlanAction> findPlanActionsByStatus(StatutEnum statut);
    List<PlanAction> findPlanActionsByNonConformeId(UUID nonConformeId);
    @Query(value = "SELECT " +
            "EXTRACT(MONTH FROM created_at) AS mois, " +
            "status, " +
            "COUNT(*) AS total " +
            "FROM plan_action " +
            "WHERE created_at BETWEEN :debut AND :fin " +
            "GROUP BY EXTRACT(MONTH FROM created_at), status " +
            "ORDER BY EXTRACT(MONTH FROM created_at)",
            nativeQuery = true)
    List<Object[]> countStatusByMonth(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);
    PlanAction findPlanActionByNumeroNc(String numeroNc);

    @Query("SELECT p FROM PlanAction p, NonConformite n WHERE p.nonConformeId = n.id AND (n.structureSoumissionId = :structureId OR n.origineId = :structureId)")
    List<PlanAction> findPlanActionsByStructureId(@Param("structureId") String structureId);
}
