package com.qualiapproche.repository;

import com.qualiapproche.entities.NiveauNonConformite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NiveauNonConformiteRepository extends JpaRepository<NiveauNonConformite, UUID> {
}
