package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.DomaineApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DomaineApplicationRepository extends JpaRepository<DomaineApplication, UUID> {
}
