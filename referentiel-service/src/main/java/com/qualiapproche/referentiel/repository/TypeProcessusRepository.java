package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.TypeProcessus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TypeProcessusRepository extends JpaRepository<TypeProcessus, UUID> {
}
