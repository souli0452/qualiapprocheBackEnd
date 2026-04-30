package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
public interface FournisseurRepository extends JpaRepository<Fournisseur, UUID> {

    @Query("SELECT f FROM Fournisseur f LEFT JOIN FETCH f.criteresEvaluation WHERE f.id = :fournisseurId")
    Fournisseur findByIdWithCriteres(@Param("fournisseurId") UUID fournisseurId);
}
