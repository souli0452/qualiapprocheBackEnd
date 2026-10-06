package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.NotationCritere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotationCritereRepository extends JpaRepository<NotationCritere, UUID> {

    /** Les critères publiés d'un audit qui n'ont pas encore de dossier dans le module Non-conformités. */
    @Query("SELECT n FROM NotationCritere n WHERE n.evaluation.audit.id = :auditId "
            + "AND n.statut = com.qualiapproche.evaluation.enumeration.StatutNotation.PUBLIE "
            + "AND n.nonConformiteId IS NULL")
    List<NotationCritere> publieesSansDossier(@Param("auditId") UUID auditId);

    /** Les critères publiés d'une direction, tous audits confondus, du plus récent au plus ancien. */
    @Query("SELECT n FROM NotationCritere n WHERE n.evaluation.directionId = :directionId "
            + "AND n.statut = com.qualiapproche.evaluation.enumeration.StatutNotation.PUBLIE "
            + "ORDER BY n.evaluation.dateEvaluation DESC")
    List<NotationCritere> publieesDeLaDirection(@Param("directionId") UUID directionId);
}
