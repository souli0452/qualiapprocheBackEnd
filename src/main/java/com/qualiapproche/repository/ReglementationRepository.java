package com.qualiapproche.repository;

import com.qualiapproche.entities.Reglementation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReglementationRepository extends JpaRepository<Reglementation, UUID> {
}
