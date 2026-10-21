package com.qualiapproche.common.messagerie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Ce qu'exige une publication sûre : trouver les messages échus, en revendiquer un, purger. */
@NoRepositoryBean
public interface MessageSortantRepository<E extends AbstractMessageSortant> extends JpaRepository<E, UUID> {

    /** Les messages dont l'heure est venue, les plus anciens d'abord. */
    @Query(value = "SELECT id FROM message_sortant WHERE statut IN ('A_PUBLIER', 'EN_COURS') "
            + "AND prochaine_tentative_at <= :maintenant ORDER BY cree_le LIMIT :lot", nativeQuery = true)
    List<UUID> echus(@Param("maintenant") LocalDateTime maintenant, @Param("lot") int lot);

    /**
     * Revendique un message, en une seule instruction : s'il est encore reprenable, il passe
     * EN_COURS, sa tentative est comptée et son échéance repoussée — si la publication
     * s'interrompt, il redevient reprenable à cette échéance.
     *
     * @return 1 si la revendication est acquise, 0 si une autre publication l'a déjà pris
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE #{#entityName} m SET m.statut = :enCours, m.tentatives = m.tentatives + 1, "
            + "m.prochaineTentativeAt = :reprise "
            + "WHERE m.id = :id AND m.statut IN :reprenables AND m.prochaineTentativeAt <= :maintenant")
    int revendiquer(@Param("id") UUID id, @Param("enCours") StatutMessage enCours,
                    @Param("reprenables") Collection<StatutMessage> reprenables,
                    @Param("maintenant") LocalDateTime maintenant, @Param("reprise") LocalDateTime reprise);

    long countByStatut(StatutMessage statut);

    /** Les messages publiés depuis longtemps : la table ne grossit pas sans fin. */
    @Modifying
    @Query("DELETE FROM #{#entityName} m WHERE m.statut = :publie AND m.publieLe < :avant")
    int purgerLesPublies(@Param("publie") StatutMessage publie, @Param("avant") LocalDateTime avant);

    /**
     * Les messages confidentiels déposés depuis longtemps, publiés ou non : un secret qui n'a pas
     * pu partir à temps ne reste pas en base indéfiniment.
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} m WHERE m.type IN :types AND m.creeLe < :avant")
    int purgerParType(@Param("types") Collection<String> types, @Param("avant") LocalDateTime avant);
}
