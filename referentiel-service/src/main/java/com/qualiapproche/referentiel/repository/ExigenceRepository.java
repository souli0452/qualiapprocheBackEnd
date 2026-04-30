package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Exigence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExigenceRepository extends JpaRepository<Exigence, UUID> {
}
