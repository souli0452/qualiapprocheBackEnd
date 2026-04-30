package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Produit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProduitRepository extends JpaRepository<Produit, UUID> {
}
