package com.qualiapproche.repository;

import com.qualiapproche.entities.TypeNonConformite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TypeNonConformiteRepository extends JpaRepository<TypeNonConformite, UUID> {
}
