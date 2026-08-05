package com.qualiapproche.amelioration.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.qualiapproche.common.utils.StatutEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.amelioration.entities.PlanAction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlanActionRepository extends JpaRepository<PlanAction, UUID>, JpaSpecificationExecutor<PlanAction> {
    List<PlanAction> findPlanActionsByResponsableEmailAndStatus(String responsableEmail, StatutEnum statut);
    Page<PlanAction> findPlanActionsByResponsableEmailAndStatus(String responsableEmail, StatutEnum statut, Pageable pageable);
    List<PlanAction> findPlanActionsByResponsableEmail(String responsableEmail);
    Page<PlanAction> findPlanActionsByResponsableEmail(String responsableEmail, Pageable pageable);
    List<PlanAction> findPlanActionsByStatus(StatutEnum statut);
    List<PlanAction> findPlanActionsByNonConformeId(UUID nonConformeId);

    /** Actions désignées par le moteur comme ouvertes à une décision de l'appelant. */
    Page<PlanAction> findByIdIn(java.util.Collection<UUID> ids, Pageable pageable);
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

    @Query("SELECT p FROM PlanAction p, NonConformite n WHERE p.nonConformeId = n.id "
            + "AND (n.structureSoumissionId = :structureId OR n.origineId = :structureId)")
    List<PlanAction> findPlanActionsByStructureId(@Param("structureId") String structureId);

    @Query("SELECT p FROM PlanAction p, NonConformite n WHERE p.nonConformeId = n.id "
            + "AND (n.structureSoumissionId = :structureId OR n.origineId = :structureId)")
    Page<PlanAction> findPlanActionsByStructureId(@Param("structureId") String structureId, Pageable pageable);
}
