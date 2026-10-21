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

    /**
     * {@link #realisesDepuis} pour toute une page d'auditeurs : les audits qu'ils ont menés comme
     * responsables d'équipe. Chaque ligne porte l'identifiant de l'auditeur puis son compte.
     */
    @Query("SELECT a.responsableEquipe.id, COUNT(a) FROM Audit a "
            + "WHERE a.responsableEquipe.id IN :auditeurIds "
            + "AND a.statut = com.qualiapproche.evaluation.enumeration.StatutAudit.CLOTURE "
            + "AND a.dateFinPrevue >= :depuis GROUP BY a.responsableEquipe.id")
    List<Object[]> realisesCommeResponsablesDepuis(@Param("auditeurIds") Collection<UUID> auditeurIds,
                                                   @Param("depuis") LocalDate depuis);

    /**
     * Le complément : les audits où ils figuraient parmi les auditeurs associés. Un audit dont
     * l'auditeur était aussi le responsable est écarté, pour ne pas le compter deux fois.
     */
    @Query("SELECT m.id, COUNT(DISTINCT a) FROM Audit a JOIN a.membresEquipe m "
            + "WHERE m.id IN :auditeurIds "
            + "AND (a.responsableEquipe IS NULL OR a.responsableEquipe.id <> m.id) "
            + "AND a.statut = com.qualiapproche.evaluation.enumeration.StatutAudit.CLOTURE "
            + "AND a.dateFinPrevue >= :depuis GROUP BY m.id")
    List<Object[]> realisesCommeMembresDepuis(@Param("auditeurIds") Collection<UUID> auditeurIds,
                                              @Param("depuis") LocalDate depuis);
}
