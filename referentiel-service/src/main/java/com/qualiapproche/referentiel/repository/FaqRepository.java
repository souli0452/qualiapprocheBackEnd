package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FaqRepository extends JpaRepository<Faq, UUID> {

    /** Ce que l'aide affiche et ce que l'assistant récite : les entrées publiées, dans l'ordre. */
    List<Faq> findAllByDirectionIdAndPublieeTrueOrderByRangAscCreatedAtAsc(UUID directionId);

    /** Ce que l'écran d'administration montre : tout, publié ou non, filtré par la recherche. */
    @Query("SELECT f FROM Faq f WHERE f.directionId = :directionId AND (:recherche IS NULL "
            + "OR LOWER(f.question) LIKE LOWER(CONCAT('%', :recherche, '%')) "
            + "OR LOWER(f.reponse) LIKE LOWER(CONCAT('%', :recherche, '%')) "
            + "OR LOWER(f.categorie) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Faq> rechercher(@Param("directionId") UUID directionId,
                         @Param("recherche") String recherche, Pageable pageable);
}
