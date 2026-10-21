package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.EvaluationAuditeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationAuditeurRepository extends JpaRepository<EvaluationAuditeur, UUID> {

    List<EvaluationAuditeur> findAllByAuditId(UUID auditId);

    List<EvaluationAuditeur> findAllByAuditeurId(UUID auditeurId);

    Optional<EvaluationAuditeur> findByAuditIdAndAuditeurIdAndCritereId(UUID auditId, UUID auditeurId, UUID critereId);

    /** La moyenne des niveaux reçus, sur la valeur recopiée de chacun ; nulle sans évaluation. */
    @Query("SELECT AVG(e.valeurNiveau) FROM EvaluationAuditeur e WHERE e.auditeur.id = :auditeurId")
    Double moyenne(@Param("auditeurId") UUID auditeurId);

    /**
     * Les mêmes moyennes pour toute une page d'auditeurs, en une requête : chaque ligne porte
     * l'identifiant de l'auditeur puis sa moyenne. Un auditeur jamais évalué n'a pas de ligne.
     */
    @Query("SELECT e.auditeur.id, AVG(e.valeurNiveau) FROM EvaluationAuditeur e "
            + "WHERE e.auditeur.id IN :auditeurIds GROUP BY e.auditeur.id")
    List<Object[]> moyennes(@Param("auditeurIds") Collection<UUID> auditeurIds);
}
