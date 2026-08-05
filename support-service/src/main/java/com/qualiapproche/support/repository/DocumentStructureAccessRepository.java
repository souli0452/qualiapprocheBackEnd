package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DocumentStructureAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentStructureAccessRepository extends JpaRepository<DocumentStructureAccess, UUID> {

    List<DocumentStructureAccess> findByDocumentId(UUID documentId);

    Optional<DocumentStructureAccess> findByDocumentIdAndStructureId(UUID documentId, String structureId);

    List<DocumentStructureAccess> findByStructureId(String structureId);

    void deleteByDocumentIdAndStructureId(UUID documentId, String structureId);
}
