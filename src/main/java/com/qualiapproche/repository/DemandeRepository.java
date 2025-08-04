package com.qualiapproche.repository;

import com.qualiapproche.entities.Demande;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DemandeRepository extends JpaRepository<Demande, UUID> {
}
