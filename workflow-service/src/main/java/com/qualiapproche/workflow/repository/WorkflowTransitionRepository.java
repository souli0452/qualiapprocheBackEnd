package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.model.StepDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    /**
     * Actions de cette nature offertes par l'étape.
     *
     * <p>Une liste, et non une transition unique : une étape peut proposer plusieurs suites qui
     * approuvent — valider, ou valider en demandant un complément. L'unicité portait autrefois sur
     * le couple (étape, décision) ; c'est le code de l'action qui l'identifie désormais.</p>
     */
    java.util.List<WorkflowTransition> findByFromStepIdAndDecision(Long fromStepId, StepDecision decision);

    /** L'action d'une étape désignée par son code métier. */
    Optional<WorkflowTransition> findByFromStepIdAndCode(Long fromStepId, String code);

    /** Faits déjà exigés par au moins une transition, tous circuits confondus. */
    @Query("select distinct t.conditionRequise from WorkflowTransition t where t.conditionRequise is not null")
    List<String> faitsExiges();
}
