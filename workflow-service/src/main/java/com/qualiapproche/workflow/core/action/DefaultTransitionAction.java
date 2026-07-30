package com.qualiapproche.workflow.core.action;

import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.model.ActionExecutionContext;
import com.qualiapproche.workflow.core.model.Transition;

/**
 * Action par defaut : deplace la donnee vers l'etat de destination de la transition.
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret de la transition
 */
public class DefaultTransitionAction<D extends IData, T extends Transition<D>>
        extends TransitionActionCore<D, T> {

    /**
     * Preparation : aucun traitement par defaut.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si la preparation echoue
     */
    @Override
    protected void before(final ActionExecutionContext<D, T> pContexte) throws WorkflowException {
        // rien par defaut
    }

    /**
     * Coeur de l'action : place la donnee dans l'etat de destination de la transition.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si la mise a jour echoue
     */
    @Override
    protected void update(final ActionExecutionContext<D, T> pContexte) throws WorkflowException {
        pContexte.getData().setEtat(pContexte.getTransition().getEtatDestination());
    }

    /**
     * Suites : aucun traitement par defaut.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si le traitement des suites echoue
     */
    @Override
    protected void after(final ActionExecutionContext<D, T> pContexte) throws WorkflowException {
        // rien par defaut
    }
}
