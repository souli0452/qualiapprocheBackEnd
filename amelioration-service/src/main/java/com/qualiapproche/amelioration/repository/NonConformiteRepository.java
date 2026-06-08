package com.qualiapproche.amelioration.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.qualiapproche.common.dto.NcStats;
import com.qualiapproche.common.dto.NonConformiteByStructDto;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NonConformiteRepository extends JpaRepository<NonConformite, UUID>, JpaSpecificationExecutor<NonConformite> {

        Page<NonConformite> findByEtatTraitement(Etat etat, Pageable pageable);

        Page<NonConformite> findAllByOrigineId(String origineId, Pageable pageable);

        Page<NonConformite> findAllByOrigineIdAndStatusIsNot(String origineId, Status status, Pageable pageable);

        Page<NonConformite> findAllByStructureSoumissionIdAndStatusIsNot(String structureSoumissionId, Status status, Pageable pageable);

        Page<NonConformite> findAllByEtatTraitementAndStructureSoumissionId(Etat etatTraitement,
                        String structureSoumissionId, Pageable pageable);

        Page<NonConformite> findAllByEtatTraitementAndOrigineId(Etat etatTraitement, String origineId, Pageable pageable);

        NonConformite getNonConformiteByNumeroReference(String numeroReference);

        Page<NonConformite> findByUserImputIdAndEtatTraitement(String userImputId, Etat etatTraitement, Pageable pageable);

        @Query(value = "SELECT a.status, COUNT(*) FROM quali_nc a WHERE a.structure_soumission_id = :structureId GROUP BY a.status", nativeQuery = true)
        List<NcStats> countByStatusForStructure(@Param("structureId") String structureId);

        Page<NonConformite> findAllByStatusAndStructureSoumissionId(Status status, String structureSoumissionId, Pageable pageable);

        @Query("SELECT MAX(CAST(SUBSTRING(n.numeroReference, LENGTH(n.numeroReference) - 4) AS integer)) " +
                        "FROM NonConformite n WHERE n.structureSoumissionId = :structureSoumissionId " +
                        "AND EXTRACT(YEAR FROM n.createdAt) = :annee")
        Integer findDernierNumero(@Param("structureSoumissionId") String structureSoumissionId, @Param("annee") int annee);

        @Query(value = "SELECT a.origine_service_libelle_court, COUNT(*) as count " +
                        "FROM quali_nc a " +
                        "WHERE a.created_at BETWEEN :debut AND :fin " +
                        "GROUP BY a.origine_service_libelle_court", nativeQuery = true)
        List<NonConformiteByStructDto> getNonConformiteStatsByStructureAndPeriod(
                        @Param("debut") LocalDateTime debut,
                        @Param("fin") LocalDateTime fin);

        @Query(value = "SELECT EXTRACT(MONTH FROM created_at) AS mois, COUNT(*) AS total " +
                        "FROM quali_nc " +
                        "WHERE EXTRACT(YEAR FROM created_at) = :annee " +
                        "GROUP BY EXTRACT(MONTH FROM created_at)", nativeQuery = true)
        List<Object[]> countByMonth(@Param("annee") int annee);

        @Query(value = "SELECT " +
                        "EXTRACT(MONTH FROM created_at) AS mois, " +
                        "status, " + // Utilisez directement le nom de la colonne enum
                        "COUNT(*) AS total " +
                        "FROM quali_nc  " +
                        "WHERE created_at BETWEEN :debut AND :fin " +
                        "AND status IN :statuts " + // Filtre par les valeurs enum
                        "GROUP BY EXTRACT(MONTH FROM created_at), status", nativeQuery = true)
        List<Object[]> countByMonthAndStatus(
                        @Param("debut") LocalDateTime debut,
                        @Param("fin") LocalDateTime fin,
                        @Param("statuts") List<String> statuts);

        // Méthode de debug
        @Query("SELECT n.status, COUNT(n) FROM NonConformite n " +
                        "WHERE n.createdAt BETWEEN :debut AND :fin " +
                        "GROUP BY n.status")
        List<Object[]> countStatusForDebug(
                        @Param("debut") LocalDateTime debut,
                        @Param("fin") LocalDateTime fin);

        @Query(value = "SELECT " +
                        "EXTRACT(MONTH FROM created_at) AS mois, " +
                        "COUNT(*) AS total " +
                        "FROM quali_nc " +
                        "WHERE created_at BETWEEN :debut AND :fin " +
                        "AND origine_id = :origineServiceId " +
                        "GROUP BY EXTRACT(MONTH FROM created_at) " +
                        "ORDER BY EXTRACT(MONTH FROM created_at)", nativeQuery = true)
        List<Object[]> countByMonthAndService(
                        @Param("debut") LocalDateTime debut,
                        @Param("fin") LocalDateTime fin,
                        @Param("origineServiceId") String origineServiceId);

        @Query(value = "SELECT " +
                        "EXTRACT(MONTH FROM created_at) AS mois, " +
                        "status, " + // Utilisez directement le nom de la colonne enum
                        "COUNT(*) AS total " +
                        "FROM quali_nc  " +
                        "WHERE created_at BETWEEN :debut AND :fin " +
                        "AND origine_id = :origineServiceId " +
                        "AND status IN :statuts " + // Filtre par les valeurs enum
                        "GROUP BY EXTRACT(MONTH FROM created_at), status", nativeQuery = true)
        List<Object[]> countByMonthAndStatusService(
                        @Param("debut") LocalDateTime debut,
                        @Param("fin") LocalDateTime fin,
                        @Param("statuts") List<String> statuts,
                        @Param("origineServiceId") String origineServiceId);

        @Query(value = "SELECT " +
                        "EXTRACT(MONTH FROM created_at) AS mois, " +
                        "niveau_non_conformite_libelle AS niveau, " +
                        "COUNT(*) AS total " +
                        "FROM quali_nc " +
                        "WHERE created_at BETWEEN :debut AND :fin " +
                        "AND origine_id = :origineServiceId " +
                        "AND niveau_non_conformite_libelle IS NOT NULL " +
                        "GROUP BY EXTRACT(MONTH FROM created_at), niveau_non_conformite_libelle " +
                        "ORDER BY EXTRACT(MONTH FROM created_at)", nativeQuery = true)
        List<Object[]> countByMonthAndNiveau(
                        @Param("debut") LocalDateTime debut,
                        @Param("fin") LocalDateTime fin,
                        @Param("origineServiceId") String origineServiceId);
        Page<NonConformite> findAllByCreatedById(String userId, Pageable pageable);

        Page<NonConformite> findAllByCreatedByIdAndStatusIn(String createdById, List<Status> statuses, Pageable pageable);

        Page<NonConformite> findAllByUserImputId(String userImputId, Pageable pageable);

        Page<NonConformite> findAllByCreatedByIdAndStatus(String createdById, Status status, Pageable pageable);

        Page<NonConformite> findAllByStructureSoumissionIdOrOrigineId(String structureSoumissionId, String originId, Pageable pageable);

        Page<NonConformite> findAllByCurrentUserStructure(String structureId, Pageable pageable);

        long countByCreatedByIdAndStatus(String userId, Status status);

        long countByUserImputId(String userId);

        @Query("SELECT n FROM NonConformite n WHERE n.createdById = :userId OR n.userImputId = :userId")
        Page<NonConformite> findAllByUserInvolved(@Param("userId") String userId, Pageable pageable);

        @Query(value = "SELECT EXTRACT(MONTH FROM created_at) as mois, niveau_non_conformite_libelle as gravite, COUNT(*) as count " +
                        "FROM quali_nc " +
                        "WHERE EXTRACT(YEAR FROM created_at) = :annee " +
                        "AND status <> 'DRAFT' " +
                        "AND (:structureId IS NULL OR :structureId = '' OR structure_soumission_id = :structureId OR origine_id = :structureId) " +
                        "GROUP BY EXTRACT(MONTH FROM created_at), niveau_non_conformite_libelle", nativeQuery = true)
        List<Object[]> getEvolutionStatsByYear(
                        @Param("annee") int annee,
                        @Param("structureId") String structureId);

        @Query(value = "SELECT COUNT(*) " +
                        "FROM quali_nc " +
                        "WHERE EXTRACT(YEAR FROM created_at) = :annee " +
                        "AND status <> 'DRAFT' " +
                        "AND (:structureId IS NULL OR :structureId = '' OR structure_soumission_id = :structureId OR origine_id = :structureId)", nativeQuery = true)
        long countTotalByYear(
                        @Param("annee") int annee,
                        @Param("structureId") String structureId);

        @Query(value = "SELECT CEIL(EXTRACT(DAY FROM created_at) / 7.0) as semaine, niveau_non_conformite_libelle as gravite, COUNT(*) as count " +
                        "FROM quali_nc " +
                        "WHERE EXTRACT(YEAR FROM created_at) = :annee " +
                        "AND EXTRACT(MONTH FROM created_at) = :mois " +
                        "AND status <> 'DRAFT' " +
                        "AND (:structureId IS NULL OR :structureId = '' OR structure_soumission_id = :structureId OR origine_id = :structureId) " +
                        "GROUP BY CEIL(EXTRACT(DAY FROM created_at) / 7.0), niveau_non_conformite_libelle", nativeQuery = true)
        List<Object[]> getEvolutionStatsByMonth(
                        @Param("annee") int annee,
                        @Param("mois") int mois,
                        @Param("structureId") String structureId);

        @Query(value = "SELECT COUNT(*) " +
                        "FROM quali_nc " +
                        "WHERE EXTRACT(YEAR FROM created_at) = :annee " +
                        "AND EXTRACT(MONTH FROM created_at) = :mois " +
                        "AND status <> 'DRAFT' " +
                        "AND (:structureId IS NULL OR :structureId = '' OR structure_soumission_id = :structureId OR origine_id = :structureId)", nativeQuery = true)
        long countTotalByMonth(
                        @Param("annee") int annee,
                        @Param("mois") int mois,
                        @Param("structureId") String structureId);

        Page<NonConformite> findAllByNiveauNonConformiteId(java.util.UUID niveauNonConformiteId, Pageable pageable);
}

        

        

        

        

        

        

        

        
        