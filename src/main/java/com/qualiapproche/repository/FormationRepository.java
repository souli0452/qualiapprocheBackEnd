package com.qualiapproche.repository;

import com.qualiapproche.entities.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FormationRepository extends JpaRepository<Formation, UUID>{
}
