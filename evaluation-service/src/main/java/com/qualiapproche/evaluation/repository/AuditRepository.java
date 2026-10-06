package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<Audit, UUID>, JpaSpecificationExecutor<Audit> {

    /** Les audits encore vivants, toutes directions confondues : la surveillance de nuit. */
    List<Audit> findAllByStatutIn(Collection<StatutAudit> statuts);

    /**
     * Les audits vivants d'un auditeur qui chevauchent une période.
     *
     * <p>Deux périodes se chevauchent quand chacune commence avant que l'autre finisse.</p>
     */
    @Query("SELECT DISTINCT a FROM Audit a LEFT JOIN a.membresEquipe m "
            + "WHERE (a.responsableEquipe.id = :auditeurId OR m.id = :auditeurId) "
            + "AND a.statut NOT IN (com.qualiapproche.evaluation.enumeration.StatutAudit.CLOTURE, "
            + "com.qualiapproche.evaluation.enumeration.StatutAudit.ANNULE) "
            + "AND a.dateDebutPrevue <= :fin AND a.dateFinPrevue >= :debut")
    List<Audit> chevauchant(@Param("auditeurId") UUID auditeurId,
                            @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /** Les audits clôturés d'un auditeur dont la fin prévue tombe après la date donnée. */
    @Query("SELECT COUNT(DISTINCT a) FROM Audit a LEFT JOIN a.membresEquipe m "
            + "WHERE (a.responsableEquipe.id = :auditeurId OR m.id = :auditeurId) "
            + "AND a.statut = com.qualiapproche.evaluation.enumeration.StatutAudit.CLOTURE "
            + "AND a.dateFinPrevue >= :depuis")
    long realisesDepuis(@Param("auditeurId") UUID auditeurId, @Param("depuis") LocalDate depuis);
}
