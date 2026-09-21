package com.qualiapproche.ia.repository;

import com.qualiapproche.ia.entities.MessageIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageIaRepository extends JpaRepository<MessageIa, UUID> {

    /** Le fil entier, dans l'ordre où il s'est tenu. */
    List<MessageIa> findAllByConversationIdOrderByRangAsc(UUID conversationId);

    /**
     * Les derniers messages du fil, le plus récent d'abord — l'appelant les remet à l'endroit.
     *
     * <p>C'est la fenêtre envoyée au modèle : un fil entier repartirait à chaque tour, et son coût
     * croîtrait avec sa longueur jusqu'à épuiser le budget d'une structure en une conversation.</p>
     */
    List<MessageIa> findByConversationIdOrderByRangDesc(UUID conversationId,
                                                       org.springframework.data.domain.Pageable fenetre);

    /**
     * Jetons consommés par les conversations d'une direction depuis {@code debutJour}.
     *
     * <p>Même budget que les suggestions : les deux usages puisent au même forfait, et compter
     * l'un sans l'autre laisserait la conversation le vider sans jamais être refusée.</p>
     */
    @Query("SELECT COALESCE(SUM(m.jetonsUtilises), 0) FROM MessageIa m "
            + "WHERE m.directionId = :directionId AND m.createdAt >= :debutJour")
    Long sommeJetonsDepuisParDirection(@Param("directionId") UUID directionId,
                                       @Param("debutJour") LocalDateTime debutJour);

    /** Même décompte, rabattu sur l'utilisateur quand le jeton ne porte pas de direction. */
    @Query("SELECT COALESCE(SUM(m.jetonsUtilises), 0) FROM MessageIa m "
            + "WHERE m.createdById = :createdById AND m.createdAt >= :debutJour")
    Long sommeJetonsDepuisParUtilisateur(@Param("createdById") String createdById,
                                         @Param("debutJour") LocalDateTime debutJour);
}
