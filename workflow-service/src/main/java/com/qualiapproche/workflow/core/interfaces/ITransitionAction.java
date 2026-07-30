package com.qualiapproche.workflow.core.interfaces;

import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.model.ActionExecutionContext;
import com.qualiapproche.workflow.core.model.Transition;

/**
 * Traitement declenche par le franchissement d'une transition.
 *
 * <p>Contrairement a l'implementation historique — ou l'interface etait un type brut —
 * les parametres {@code D} et {@code T} sont explicites : une action recoit un contexte
 * deja type, sans transtypage.</p>
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret de la transition, pour acceder a ses attributs specifiques
 */
@FunctionalInterface
public interface ITransitionAction<D extends IData, T extends Transition<D>> {

    /**
     * Execute le traitement associe au franchissement de la transition.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si l'action echoue
     */
    void executerAction(ActionExecutionContext<D, T> pContexte) throws WorkflowException;
}
