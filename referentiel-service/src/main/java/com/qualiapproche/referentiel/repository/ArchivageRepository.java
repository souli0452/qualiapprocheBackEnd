package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Archivage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArchivageRepository extends JpaRepository<Archivage, UUID> {
}
