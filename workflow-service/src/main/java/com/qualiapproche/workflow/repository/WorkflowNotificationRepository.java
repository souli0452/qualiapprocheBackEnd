package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowNotificationRepository extends JpaRepository<WorkflowNotification, UUID> {

    /**
     * Notifications reprenables : en attente, ou revendiquées par un ouvrier qui n'a pas donné
     * suite avant l'échéance de sa revendication.
     */
    @Query("select n from WorkflowNotification n "
            + "where n.statut in :statuts and n.prochaineTentativeAt <= :echeance "
            + "order by n.createdAt asc")
    List<WorkflowNotification> aReprendre(
            @Param("statuts") Collection<WorkflowNotification.NotificationStatut> statuts,
            @Param("echeance") LocalDateTime echeance,
            Pageable pageable);

    /**
     * Revendique une notification : c'est ce qui garantit qu'elle n'est postée qu'une seule fois.
     *
     * <p>Le passage au statut « en cours de remise » et l'incrément du compteur se font en une
     * seule instruction conditionnelle. Deux ouvriers qui visent la même ligne sont donc
     * départagés par la base : le second voit la condition devenue fausse et obtient zéro ligne
     * modifiée. Sans cela, l'ordonnanceur et la remise immédiate d'après commit pouvaient lire
     * tous deux le statut « à remettre » et appeler le service métier chacun de leur côté.</p>
     *
     * <p>{@code prochaineTentativeAt} reçoit l'échéance de reprise : si l'ouvrier s'interrompt
     * — arrêt du pod, coupure réseau —, la notification redevient reprenable à ce moment-là
     * plutôt que de rester indéfiniment bloquée sur une revendication sans suite.</p>
     *
     * @return 1 si la revendication est acquise, 0 si un autre ouvrier a été plus rapide
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update WorkflowNotification n "
            + "set n.statut = :enCours, n.tentatives = n.tentatives + 1, "
            + "    n.prochaineTentativeAt = :echeanceReprise "
            + "where n.id = :id and n.statut in :statutsReprenables "
            + "  and n.prochaineTentativeAt <= :maintenant")
    int revendiquer(@Param("id") UUID id,
                    @Param("enCours") WorkflowNotification.NotificationStatut enCours,
                    @Param("statutsReprenables") Collection<WorkflowNotification.NotificationStatut> statutsReprenables,
                    @Param("maintenant") LocalDateTime maintenant,
                    @Param("echeanceReprise") LocalDateTime echeanceReprise);

    long countByStatut(WorkflowNotification.NotificationStatut statut);
}
