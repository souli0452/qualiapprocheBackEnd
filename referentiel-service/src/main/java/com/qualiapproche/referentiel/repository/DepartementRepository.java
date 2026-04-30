package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartementRepository extends JpaRepository<Departement, UUID> {
}
