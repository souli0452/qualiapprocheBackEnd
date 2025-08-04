package com.qualiapproche.repository;

import com.qualiapproche.entities.TypeProcessus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TypeProcessusRepository extends JpaRepository<TypeProcessus, UUID> {
}
