package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, UUID> {

    List<Signature> findAllByAuditIdOrderByOrdreAsc(UUID auditId);

    void deleteAllByAuditId(UUID auditId);
}
