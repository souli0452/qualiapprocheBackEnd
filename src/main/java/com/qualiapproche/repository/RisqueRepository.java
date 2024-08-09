package com.qualiapproche.repository;

import com.qualiapproche.entities.Risque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RisqueRepository extends JpaRepository<Risque, UUID> {
}
