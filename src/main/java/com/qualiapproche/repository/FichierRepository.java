package com.qualiapproche.repository;

import com.qualiapproche.entities.Fichier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FichierRepository extends JpaRepository<Fichier, UUID> {
}
