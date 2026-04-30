package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.ContratAccord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContratAccordRepository extends JpaRepository<ContratAccord, UUID> {
}
