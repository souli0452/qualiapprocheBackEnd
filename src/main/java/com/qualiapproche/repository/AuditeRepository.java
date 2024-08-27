package com.qualiapproche.repository;

import com.qualiapproche.dto.AuditeDto;
import com.qualiapproche.entities.Audite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditeRepository extends JpaRepository<Audite, UUID> {
}
