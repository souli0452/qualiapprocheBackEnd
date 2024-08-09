package com.qualiapproche.repository;

import com.qualiapproche.entities.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FournisseurRepository extends JpaRepository<Fournisseur, UUID> {
}
