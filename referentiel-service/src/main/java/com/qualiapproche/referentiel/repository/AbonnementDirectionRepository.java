package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.AbonnementDirection;
import com.qualiapproche.referentiel.entities.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbonnementDirectionRepository extends JpaRepository<AbonnementDirection, UUID> {
    Optional<AbonnementDirection> findByDirection(Structure direction);
    Optional<AbonnementDirection> findByDirectionId(UUID directionId);
}
