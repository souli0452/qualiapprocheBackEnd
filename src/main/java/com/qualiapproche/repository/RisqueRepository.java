package com.qualiapproche.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qualiapproche.entities.Risque;

public interface RisqueRepository extends JpaRepository<Risque, UUID> {
}
