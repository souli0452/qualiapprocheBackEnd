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

    /** Ce que l'aide affiche et ce que l'assistant récite : les publiées, dans l'ordre d'écriture. */
    List<Faq> findAllByDirectionIdAndPublieeTrueOrderByCreatedAtAsc(UUID directionId);

    /** Ce que l'écran d'administration montre sans filtre : tout, publié ou non. */
    List<Faq> findAllByDirectionIdOrderByCreatedAtAsc(UUID directionId);

    /**
     * Ce que l'écran d'administration montre d'un côté ou de l'autre, filtré par la recherche.
     *
     * <p>La publication n'est pas facultative : l'écran présente deux onglets, publiées et non
     * publiées, et demande donc toujours l'un des deux. Pas de troisième état à représenter,
     * donc pas de paramètre nul à lier — celui de la recherche a suffi à nous instruire.</p>
     *
     * <p>Le terme n'est <b>jamais nul</b>, et la requête ne le teste donc pas. Un
     * {@code :recherche IS NULL} paraissait plus expressif ; il obligeait à lier un nul sans
     * type, que PostgreSQL rangeait en {@code bytea} avant de chercher un {@code lower(bytea)}
     * qui n'existe pas — la liste entière échouait en 500 avant même qu'une question soit
     * écrite. Une chaîne vide donne {@code LIKE '%%'}, qui retient tout, et le cas particulier
     * disparaît avec elle.</p>
     */
    @Query("SELECT f FROM Faq f WHERE f.directionId = :directionId "
            + "AND f.publiee = :publiee "
            + "AND (LOWER(f.question) LIKE LOWER(CONCAT('%', :recherche, '%')) "
            + "OR LOWER(f.reponse) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Faq> rechercher(@Param("directionId") UUID directionId,
                         @Param("publiee") boolean publiee,
                         @Param("recherche") String recherche, Pageable pageable);

    /** Combien d'entrées de chaque côté : les onglets portent leur compte. */
    long countByDirectionIdAndPubliee(UUID directionId, boolean publiee);
}
