package com.qualiapproche.common.courriel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Ce qu'exige une tournée d'envoi sûre : trouver les courriels échus, en revendiquer un. */
@NoRepositoryBean
public interface CourrielSortantRepository<E extends AbstractCourrielSortant> extends JpaRepository<E, UUID> {

    /** Les courriels dont l'heure est venue, les plus anciens d'abord. */
    @Query(value = "SELECT id FROM courriel_sortant WHERE statut IN ('A_ENVOYER', 'EN_COURS') "
            + "AND prochaine_tentative_at <= :maintenant ORDER BY cree_le LIMIT :lot", nativeQuery = true)
    List<UUID> echus(@Param("maintenant") LocalDateTime maintenant, @Param("lot") int lot);

    /**
     * Revendique un courriel pour cette tournée, en une seule instruction : s'il est encore
     * reprenable, il passe EN_COURS, sa tentative est comptée et son échéance repoussée — si la
     * tournée s'interrompt (arrêt, panne), il redevient reprenable à cette échéance.
     *
     * @return 1 si la revendication est acquise, 0 si une autre tournée l'a déjà pris
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE #{#entityName} c SET c.statut = :enCours, c.tentatives = c.tentatives + 1, "
            + "c.prochaineTentativeAt = :reprise "
            + "WHERE c.id = :id AND c.statut IN :reprenables AND c.prochaineTentativeAt <= :maintenant")
    int revendiquer(@Param("id") UUID id, @Param("enCours") StatutCourriel enCours,
                    @Param("reprenables") Collection<StatutCourriel> reprenables,
                    @Param("maintenant") LocalDateTime maintenant, @Param("reprise") LocalDateTime reprise);

    long countByStatut(StatutCourriel statut);

    /** Les courriels remis depuis longtemps : la table ne grossit pas sans fin. */
    @Modifying
    @Query("DELETE FROM #{#entityName} c WHERE c.statut = :envoye AND c.envoyeLe < :avant")
    int purgerLesEnvoyes(@Param("envoye") StatutCourriel envoye, @Param("avant") LocalDateTime avant);
}
