package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DemandeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DemandeDocumentRepository extends JpaRepository<DemandeDocument, UUID> {

    List<DemandeDocument> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    List<DemandeDocument> findByStructureIdOrderByCreatedAtDesc(String structureId);

    List<DemandeDocument> findByDemandeurIdOrderByCreatedAtDesc(String demandeurId);

    List<DemandeDocument> findAllByOrderByCreatedAtDesc();
}
