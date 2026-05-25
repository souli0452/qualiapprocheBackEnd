package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.QmsDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QmsDocumentVersionRepository extends JpaRepository<QmsDocumentVersion, Long> {
    List<QmsDocumentVersion> findByDocumentIdOrderByDateCreationDesc(UUID documentId);
}
