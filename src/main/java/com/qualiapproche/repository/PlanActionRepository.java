package com.qualiapproche.repository;

import com.qualiapproche.utils.StatutEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.entities.PlanAction;

public interface PlanActionRepository extends JpaRepository<PlanAction, UUID> {
    List<PlanAction> findPlanActionsByResponsableEmailAndStatus(String responsableEmail, StatutEnum statut);
    List<PlanAction> findPlanActionsByResponsableEmail(String responsableEmail);
}
