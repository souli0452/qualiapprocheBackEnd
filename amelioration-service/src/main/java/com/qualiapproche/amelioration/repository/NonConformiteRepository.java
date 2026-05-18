package com.qualiapproche.amelioration.repository;

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

public interface NonConformiteRepository extends JpaRepository<NonConformite, UUID> {

        List<NonConformite> findByEtatTraitement(Etat etat);

        List<NonConformite> findAllByOrigineId(String origineId);

        List<NonConformite> findAllByOrigineIdAndStatusIsNot(String origineId, Status status);

        List<NonConformite> findAllByStructureSoumissionIdAndStatusIsNot(String structureSoumissionId, Status status);

        List<NonConformite> findAllByEtatTraitementAndStructureSoumissionId(Etat etatTraitement,
                        String structureSoumissionId);

        List<NonConformite> findAllByEtatTraitementAndOrigineId(Etat etatTraitement, String origineId);

        NonConformite getNonConformiteByNumeroReference(String numeroReference);

        List<NonConformite> findByUserImputIdAndEtatTraitement(String userImputId, Etat etatTraitement);

        @Query(value = "SELECT a.status, COUNT(*) FROM quali_nc a WHERE a.structure_soumission_id = :structureId GROUP BY a.status", nativeQuery = true)
        List<NcStats> countByStatusForStructure(@Param("structureId") String structureId);

        List<NonConformite> findAllByStatusAndStructureSoumissionId(Status status, String structureSoumissionId);

        @Query("SELECT MAX(CAST(SUBSTRING(n.numeroReference, LENGTH(n.numeroReference) - 4) AS integer)) " +
                        "FROM NonConformite n WHERE n.origineServiceLibelleCourt = :service " +
                        "AND EXTRACT(YEAR FROM n.createdAt) = :annee")
        Integer findDernierNumero(@Param("service") String service, @Param("annee") int annee);

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

        List<NonConformite> findAllByCreatedById(String userId);}

        

        

        

        

        

        

        

        
        