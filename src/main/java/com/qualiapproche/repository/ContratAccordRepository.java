package com.qualiapproche.repository;

import com.qualiapproche.entities.ContratAccord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContratAccordRepository extends JpaRepository<ContratAccord, UUID> {
}
