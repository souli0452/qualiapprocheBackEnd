package com.qualiapproche.repository;

import com.qualiapproche.entities.NonConformite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NonConformiteRepository extends JpaRepository<NonConformite, UUID> {
}
