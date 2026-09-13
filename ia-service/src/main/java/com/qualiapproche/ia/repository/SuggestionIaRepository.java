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
     * Les suggestions d'une direction — soit, quand le jeton ne porte pas de {@code direction_id},
     * de la structure de rattachement : {@code SecurityUtils.getCurrentDirectionId()} se rabat sur
     * {@code structure_id}, et c'est cette même clé que le budget décompte.
     *
     * <p>Appuyée par {@code idx_suggestions_ia_direction_jour}, qui porte déjà le couple
     * (direction, date) sur lequel cette liste filtre et trie.</p>
     */
    Page<SuggestionIa> findAllByDirectionId(UUID directionId, Pageable pageable);

    /**
     * Les suggestions d'une personne. Repli de la liste précédente quand le jeton ne porte aucune
     * direction, exactement comme le décompte du budget se rabat sur l'utilisateur.
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
     * Même décompte, rabattu sur l'utilisateur quand le jeton ne porte pas de direction.
     */
    @Query("SELECT COALESCE(SUM(s.jetonsUtilises), 0) FROM SuggestionIa s "
            + "WHERE s.createdById = :createdById AND s.createdAt >= :debutJour")
    Long sommeJetonsDepuisParUtilisateur(@Param("createdById") String createdById,
                                         @Param("debutJour") LocalDateTime debutJour);
}
