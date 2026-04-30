package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Reglementation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReglementationRepository extends JpaRepository<Reglementation, UUID> {
}
