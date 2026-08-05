package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowStep;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    Optional<WorkflowStep> findByNomEtapeAndWorkflow_Nom(String nomEtape, String workflowNom);
    Optional<WorkflowStep> findByNomEtapeAndWorkflow_Id(String nomEtape, java.util.UUID workflowId);

    /**
     * Étapes et leurs champs de saisie en une requête.
     *
     * <p>Sans le graphe, la collection {@code fields} de chaque étape déclenche sa propre lecture
     * différée : consulter l'état de N dossiers en coûtait autant.</p>
     */
    @EntityGraph(attributePaths = "fields")
    List<WorkflowStep> findAvecChampsByIdIn(Collection<Long> ids);

    /**
     * Étapes et leurs transitions sortantes en une requête.
     *
     * <p>Requête distincte de la précédente : joindre les deux collections dans le même graphe
     * ferait un produit cartésien, et Hibernate refuse deux {@code List} en jointure de fetch.</p>
     */
    @EntityGraph(attributePaths = {"transitions", "transitions.toStep"})
    List<WorkflowStep> findAvecTransitionsByIdIn(Collection<Long> ids);
}
