package com.qualiapproche.repository;

import com.qualiapproche.entities.Archivage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArchivageRepository extends JpaRepository<Archivage, UUID> {
}
