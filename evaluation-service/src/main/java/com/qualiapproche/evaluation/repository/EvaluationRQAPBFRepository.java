package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRQAPBFRepository extends JpaRepository<EvaluationRQAPBF, UUID> {

    List<EvaluationRQAPBF> findAllByAuditIdOrderByDateEvaluationDesc(UUID auditId);

    List<EvaluationRQAPBF> findAllByAuditIdIn(java.util.Collection<UUID> auditIds);

    List<EvaluationRQAPBF> findAllByDirectionIdAndDateEvaluationBetween(UUID directionId, java.time.LocalDate debut,
                                                                         java.time.LocalDate fin);
}
