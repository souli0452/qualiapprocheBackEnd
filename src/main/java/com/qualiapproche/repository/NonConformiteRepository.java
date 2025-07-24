package com.qualiapproche.repository;

import com.qualiapproche.dto.NcStats;
import com.qualiapproche.entities.NonConformite;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NonConformiteRepository extends JpaRepository<NonConformite, UUID> {

    List<NonConformite> findByEtatTraitement(Etat etat);
    List<NonConformite> findAllByEtatTraitementAndStructureSoumissionId(Etat etatTraitement, String structureSoumissionId);
    List<NonConformite> findAllByEtatTraitementAndOrigineId(Etat etatTraitement, String origineId);

    List<NonConformite> findByUserImputIdAndEtatTraitement(String userImputId,Etat etatTraitement);
    @Query(value = "SELECT a.status, COUNT(*) FROM quali_nc a WHERE a.structure_soumission_id = :structureId GROUP BY a.status", nativeQuery = true)
    List<NcStats> countByStatusForStructure(@Param("structureId") String structureId);


    List<NonConformite> findAllByStatusAndStructureSoumissionId(Status status,String structureSoumissionId);

    @Query("SELECT MAX(CAST(SUBSTRING(n.numeroReference, LENGTH(n.numeroReference) - 4) AS integer)) " +
            "FROM NonConformite n WHERE n.origineServiceLibelleCourt = :service " +
            "AND EXTRACT(YEAR FROM n.createdAt) = :annee")
    Integer findDernierNumero(@Param("service") String service, @Param("annee") int annee);
}
