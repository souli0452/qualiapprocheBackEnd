package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.Audite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditeRepository extends JpaRepository<Audite, UUID> {
}
