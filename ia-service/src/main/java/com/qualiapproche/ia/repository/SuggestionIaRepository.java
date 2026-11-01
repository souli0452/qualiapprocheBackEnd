package com.qualiapproche.ia.repository;

import com.qualiapproche.ia.entities.SuggestionIa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SuggestionIaRepository extends JpaRepository<SuggestionIa, UUID> {

    /**
     * Les suggestions d'une personne : ce que lit qui n'a pas la portée transverse.
     */
    Page<SuggestionIa> findAllByCreatedById(String createdById, Pageable pageable);

    /**
     * Jetons consommés par une direction depuis {@code debutJour}. Ne compte que les suggestions
     * réellement produites — les appels en échec ne laissent aucune trace à décompter.
     */
    @Query("SELECT COALESCE(SUM(s.jetonsUtilises), 0) FROM SuggestionIa s "
            + "WHERE s.directionId = :directionId AND s.createdAt >= :debutJour")
    Long sommeJetonsDepuisParDirection(@Param("directionId") UUID directionId,
                                       @Param("debutJour") LocalDateTime debutJour);

    /**
     * Même décompte, rabattu sur l'utilisateur quand l'appelant n'agit pour aucun client.
     */
    @Query("SELECT COALESCE(SUM(s.jetonsUtilises), 0) FROM SuggestionIa s "
            + "WHERE s.createdById = :createdById AND s.createdAt >= :debutJour")
    Long sommeJetonsDepuisParUtilisateur(@Param("createdById") String createdById,
                                         @Param("debutJour") LocalDateTime debutJour);
}
