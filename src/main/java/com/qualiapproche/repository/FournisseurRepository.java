package com.qualiapproche.repository;

import com.qualiapproche.entities.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
public interface FournisseurRepository extends JpaRepository<Fournisseur, UUID> {
}
