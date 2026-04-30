package com.qualiapproche.amelioration.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qualiapproche.amelioration.entities.Risque;

public interface RisqueRepository extends JpaRepository<Risque, UUID> {
}
