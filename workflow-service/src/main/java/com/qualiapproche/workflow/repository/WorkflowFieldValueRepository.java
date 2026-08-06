package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowFieldValueRepository extends JpaRepository<WorkflowFieldValue, Long> {

    /**
     * Valeurs saisies lors d'une décision.
     *
     * <p>Interrogées par identifiant d'historique plutôt qu'à travers la collection de
     * {@code ValidationHistory} : les valeurs sont enregistrées une à une après le franchissement,
     * sans être ajoutées à la collection en mémoire de l'historique déjà chargé.</p>
     */
    java.util.List<WorkflowFieldValue> findByHistory_Id(Long historyId);

    /**
     * Toutes les valeurs saisies sur les dossiers cités, la plus ancienne d'abord.
     *
     * <p>En une requête pour tout un lot : l'état du circuit est joint à chaque ligne des listes de
     * dossiers, et une lecture par ligne — voire par décision — aurait rendu ruineux l'affichage
     * d'une page.</p>
     */
    @org.springframework.data.jpa.repository.Query("""
            select v from WorkflowFieldValue v
            join fetch v.history h
            where h.validationInstance.id in :instances
            order by h.decisionDate asc, v.id asc
            """)
    java.util.List<WorkflowFieldValue> saisiesDesInstances(
            @org.springframework.data.repository.query.Param("instances")
            java.util.Collection<java.util.UUID> instances);
}
