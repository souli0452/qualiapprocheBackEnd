package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.AbonnementDirection;
import com.qualiapproche.referentiel.entities.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbonnementDirectionRepository extends JpaRepository<AbonnementDirection, UUID> {

    // Recherche par entité direction (via subscribed_direction_id en JPA)
    Optional<AbonnementDirection> findByDirection(Structure direction);

    // Recherche directe par UUID (subscribed_direction_id) — plus robuste, sans lazy loading
    @Query("SELECT a FROM AbonnementDirection a WHERE a.direction.id = :directionId")
    Optional<AbonnementDirection> findByDirectionUUID(@Param("directionId") UUID directionId);

    // Charge tous les abonnements avec leur direction en un seul appel SQL (évite le lazy loading N+1)
    @Query("SELECT a FROM AbonnementDirection a LEFT JOIN FETCH a.direction d WHERE d IS NOT NULL")
    List<AbonnementDirection> findAllWithDirection();

    // Charge le premier abonnement de type DIRECTION (la licence globale de la plateforme)
    @Query("SELECT a FROM AbonnementDirection a LEFT JOIN FETCH a.direction d WHERE d.typeStructure = 'DIRECTION' ORDER BY a.createdAt ASC")
    List<AbonnementDirection> findGlobalDirectionLicense();
}
