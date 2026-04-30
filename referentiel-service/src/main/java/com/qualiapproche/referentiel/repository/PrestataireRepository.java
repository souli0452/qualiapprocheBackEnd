package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Prestataire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrestataireRepository extends JpaRepository<Prestataire, UUID> {
}
