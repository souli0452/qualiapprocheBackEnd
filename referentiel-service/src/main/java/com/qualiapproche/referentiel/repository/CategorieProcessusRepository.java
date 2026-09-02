package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.CategorieProcessus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategorieProcessusRepository extends JpaRepository<CategorieProcessus, UUID> {
}
