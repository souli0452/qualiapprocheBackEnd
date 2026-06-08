package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.NiveauNonConformite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NiveauNonConformiteRepository extends JpaRepository<NiveauNonConformite, UUID>, JpaSpecificationExecutor<NiveauNonConformite> {
    @Query("SELECT DISTINCT n.libelle FROM NiveauNonConformite n ORDER BY n.libelle")
    List<String> findAllLibelles();
}
