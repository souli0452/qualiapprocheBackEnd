package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConstatRepository extends JpaRepository<Constat, UUID> {

    /** Ce que l'équipe voit : le compilé et le publié. */
    List<Constat> findAllByAuditIdAndStatutInOrderByDateSaisieAsc(UUID auditId, Collection<StatutConstat> statuts);

    /** Les brouillons d'un rédacteur : « Mes constats du jour ». */
    List<Constat> findAllByAuditIdAndRedacteurIdAndStatutOrderByDateSaisieAsc(
            UUID auditId, UUID redacteurId, StatutConstat statut);

    List<Constat> findAllByAuditIdAndRedacteurId(UUID auditId, UUID redacteurId);

    List<Constat> findAllByAuditId(UUID auditId);

    /** Les constats publiés générant une non-conformité, sur un ensemble d'audits. */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Constat c WHERE c.audit.id IN :auditIds "
            + "AND c.statut = com.qualiapproche.evaluation.enumeration.StatutConstat.PUBLIE "
            + "AND c.nature.genereNonConformite = true")
    long ecartsPublies(@org.springframework.data.repository.query.Param("auditIds") Collection<UUID> auditIds);

    /** Les constats publiés générant une NC d'une direction, publiés entre deux instants. */
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Constat c WHERE c.directionId = :directionId "
            + "AND c.statut = com.qualiapproche.evaluation.enumeration.StatutConstat.PUBLIE "
            + "AND c.nature.genereNonConformite = true "
            + "AND c.datePublication BETWEEN :debut AND :fin")
    List<Constat> ecartsPubliesEntre(@org.springframework.data.repository.query.Param("directionId") UUID directionId,
                                     @org.springframework.data.repository.query.Param("debut") java.time.LocalDateTime debut,
                                     @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);

    long countByAuditIdAndStatutIn(UUID auditId, Collection<StatutConstat> statuts);

    List<Constat> findAllByAuditIdAndStatutOrderByDateSaisieAsc(UUID auditId, StatutConstat statut);
}
