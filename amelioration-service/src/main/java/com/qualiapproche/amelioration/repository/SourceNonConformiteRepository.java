package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.SourceDeNonConformite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SourceNonConformiteRepository extends JpaRepository<SourceDeNonConformite, UUID>, JpaSpecificationExecutor<SourceDeNonConformite> {
}
