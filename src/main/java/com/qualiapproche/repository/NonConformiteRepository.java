package com.qualiapproche.repository;

import com.qualiapproche.entities.NonConformite;
import com.qualiapproche.enumeration.Etat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NonConformiteRepository extends JpaRepository<NonConformite, UUID> {

    List<NonConformite> findByEtatTraitement(Etat etat);

}
