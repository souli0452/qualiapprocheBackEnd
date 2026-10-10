package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.Signature;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, UUID> {

    List<Signature> findAllByAuditIdOrderByOrdreAsc(UUID auditId);

    boolean existsByAuditIdAndStatut(UUID auditId, StatutSignature statut);

    /** Les signatures qui attendent une personne, toutes directions : l'appelant filtre sur la sienne. */
    List<Signature> findAllBySignataireIdAndStatut(String signataireId, StatutSignature statut);

    void deleteAllByAuditId(UUID auditId);
}
