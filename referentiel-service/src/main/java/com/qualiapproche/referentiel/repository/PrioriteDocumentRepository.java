package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.PrioriteDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrioriteDocumentRepository extends JpaRepository<PrioriteDocument, UUID> {
}
