package com.qualiapproche.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import com.qualiapproche.entities.PlanAction;

public interface PlanActionRepository extends JpaRepository<PlanAction, UUID> {
}
