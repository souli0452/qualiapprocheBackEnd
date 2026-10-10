package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.Auditeur;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditeurRepository extends ParametrageRepository<Auditeur> {

    Optional<Auditeur> findByDirectionIdAndUtilisateurId(UUID directionId, String utilisateurId);

    boolean existsByDirectionIdAndUtilisateurId(UUID directionId, String utilisateurId);
}
