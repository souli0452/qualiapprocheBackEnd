package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.NiveauConfidentialite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NiveauConfidentialiteRepository extends JpaRepository<NiveauConfidentialite, UUID> {
}
