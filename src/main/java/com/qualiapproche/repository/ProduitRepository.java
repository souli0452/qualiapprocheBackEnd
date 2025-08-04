package com.qualiapproche.repository;

import com.qualiapproche.entities.Produit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProduitRepository extends JpaRepository<Produit, UUID> {
}
