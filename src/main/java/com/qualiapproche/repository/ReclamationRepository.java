package com.qualiapproche.repository;

import com.qualiapproche.entities.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReclamationRepository extends JpaRepository<Reclamation, UUID> {
}
