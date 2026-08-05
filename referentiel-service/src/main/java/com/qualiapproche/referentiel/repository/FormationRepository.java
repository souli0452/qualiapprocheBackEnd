package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FormationRepository extends JpaRepository<Formation, UUID> {
}
