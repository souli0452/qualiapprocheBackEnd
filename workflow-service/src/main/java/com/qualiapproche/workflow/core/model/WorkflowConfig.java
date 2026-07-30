package com.qualiapproche.workflow.core.model;

import lombok.Getter;
import lombok.Setter;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.port.output.ITransitionCondition;

import java.util.Objects;

/**
 * Associe une classe de donnee a son workflow et a sa regle d'autorisation.
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret des transitions
 * @param <W> type concret du workflow
 */
@Getter
@Setter
public class WorkflowConfig<D extends IData, T extends Transition<D>, W extends Workflow<D, T>> {

    private Class<? extends D> dataClass;
    private W workflow;
    private ITransitionCondition<D, T> transitionConditionAdapter;

    /**
     * Construit une configuration vide.
     */
    public WorkflowConfig() {
        // constructeur par defaut requis par les couches de mapping
    }

    /**
     * Construit une configuration complete.
     *
     * @param pDataClass                  classe de la donnee pilotee
     * @param pWorkflow                   workflow associe
     * @param pTransitionConditionAdapter port evaluant l'autorisation des transitions
     */
    public WorkflowConfig(final Class<? extends D> pDataClass, final W pWorkflow,
                          final ITransitionCondition<D, T> pTransitionConditionAdapter) {
        this.dataClass = Objects.requireNonNull(pDataClass, "La classe de donnee ne peut etre null.");
        this.workflow = Objects.requireNonNull(pWorkflow, "Le workflow ne peut etre null.");
        this.transitionConditionAdapter = Objects.requireNonNull(pTransitionConditionAdapter,
                "Le port de condition de transition ne peut etre null.");
    }

    /**
     * Representation lisible de la configuration.
     *
     * @return la classe de donnee et le workflow associe
     */
    @Override
    public String toString() {
        return "WorkflowConfig [dataClass=" + (this.dataClass == null ? "null" : this.dataClass.getName())
                + ", workflow=" + this.workflow + "]";
    }
}
