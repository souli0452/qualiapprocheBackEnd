package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.ChecklistAudit;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistAuditRepository extends ParametrageRepository<ChecklistAudit> {

    long countByDirectionIdAndStatut(java.util.UUID directionId,
                                     com.qualiapproche.evaluation.enumeration.StatutChecklist statut);
}
