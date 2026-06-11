package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DocumentQms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentQmsRepository extends JpaRepository<DocumentQms, UUID>, JpaSpecificationExecutor<DocumentQms> {

    Optional<DocumentQms> findByDocumentNumber(String documentNumber);

    List<DocumentQms> findByStatus(String status);

    @Query("SELECT MAX(d.documentNumber) FROM DocumentQms d WHERE d.documentNumber LIKE :prefix%")
    String findMaxDocumentNumberByPrefix(@Param("prefix") String prefix);

    // =========================================================================
    // Statistiques — répartitions (Object[] = [valeur, count])
    // =========================================================================

    /** Répartition par type de document */
    @Query("SELECT d.documentType, COUNT(d) FROM DocumentQms d WHERE d.archived = false GROUP BY d.documentType")
    List<Object[]> countByDocumentType();

    /** Répartition par statut */
    @Query("SELECT d.status, COUNT(d) FROM DocumentQms d WHERE d.archived = false GROUP BY d.status")
    List<Object[]> countByStatus();

    /** Répartition par domaine */
    @Query("SELECT d.domaine, COUNT(d) FROM DocumentQms d WHERE d.archived = false AND d.domaine IS NOT NULL GROUP BY d.domaine")
    List<Object[]> countByDomaine();

    /** Répartition par service (libellé) */
    @Query("SELECT d.serviceLibelle, COUNT(d) FROM DocumentQms d WHERE d.archived = false AND d.serviceLibelle IS NOT NULL GROUP BY d.serviceLibelle")
    List<Object[]> countByServiceLibelle();

    /** Documents en retard de révision (dateProchRevision dépassée et pas archivés) */
    @Query("SELECT COUNT(d) FROM DocumentQms d WHERE d.archived = false AND d.dateProchRevision IS NOT NULL AND d.dateProchRevision < :now")
    long countEnRetardRevision(@Param("now") LocalDateTime now);

    /** Documents confidentiels actifs */
    @Query("SELECT COUNT(d) FROM DocumentQms d WHERE d.archived = false AND d.confidentiel = true")
    long countConfidentiels();

    /** Documents externes actifs */
    @Query("SELECT COUNT(d) FROM DocumentQms d WHERE d.archived = false AND d.documentExterne = true")
    long countExternes();

    /** Total des documents non archivés */
    @Query("SELECT COUNT(d) FROM DocumentQms d WHERE d.archived = false")
    long countActifs();
}
