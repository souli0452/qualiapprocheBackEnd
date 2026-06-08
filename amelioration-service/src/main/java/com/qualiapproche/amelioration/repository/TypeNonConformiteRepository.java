package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.TypeNonConformite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TypeNonConformiteRepository extends JpaRepository<TypeNonConformite, UUID>, JpaSpecificationExecutor<TypeNonConformite> {
}
