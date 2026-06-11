package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DocumentUserAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentUserAccessRepository extends JpaRepository<DocumentUserAccess, UUID> {
    List<DocumentUserAccess> findByDocumentId(UUID documentId);
    Optional<DocumentUserAccess> findByDocumentIdAndUserId(UUID documentId, String userId);
    void deleteByDocumentIdAndUserId(UUID documentId, String userId);

    /** Tous les accès d'un utilisateur donné (ses documents partagés) */
    List<DocumentUserAccess> findByUserId(String userId);
}
