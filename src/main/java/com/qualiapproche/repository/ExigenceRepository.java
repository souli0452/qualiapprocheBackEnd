package com.qualiapproche.repository;

import com.qualiapproche.entities.Exigence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExigenceRepository extends JpaRepository<Exigence, UUID> {
}
