package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReclamationRepository extends JpaRepository<Reclamation, UUID> {
}
