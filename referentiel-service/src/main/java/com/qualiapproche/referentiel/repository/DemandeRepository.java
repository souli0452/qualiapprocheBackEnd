package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Demande;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DemandeRepository extends JpaRepository<Demande, UUID> {
}
