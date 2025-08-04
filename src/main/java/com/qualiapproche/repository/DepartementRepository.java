package com.qualiapproche.repository;

import com.qualiapproche.entities.Departement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartementRepository extends JpaRepository<Departement, UUID> {
}
