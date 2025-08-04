package com.qualiapproche.repository;

import com.qualiapproche.entities.Prestataire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrestataireRepository extends JpaRepository<Prestataire, UUID> {
}
