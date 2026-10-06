package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.EvaluationAuditeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationAuditeurRepository extends JpaRepository<EvaluationAuditeur, UUID> {

    List<EvaluationAuditeur> findAllByAuditId(UUID auditId);

    List<EvaluationAuditeur> findAllByAuditeurId(UUID auditeurId);

    Optional<EvaluationAuditeur> findByAuditIdAndAuditeurIdAndCritereId(UUID auditId, UUID auditeurId, UUID critereId);

    /** La moyenne des niveaux reçus, sur l'ordre de chaque niveau ; nulle sans évaluation. */
    @Query("SELECT AVG(e.niveau.ordre) FROM EvaluationAuditeur e WHERE e.auditeur.id = :auditeurId")
    Double moyenne(@Param("auditeurId") UUID auditeurId);
}
