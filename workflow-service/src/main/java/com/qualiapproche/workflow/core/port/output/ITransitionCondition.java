package com.qualiapproche.workflow.core.port.output;

import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.model.Transition;

/**
 * Port sortant : decide si une transition est franchissable dans un contexte donne.
 *
 * <p>C'est ici que l'application hote branche ses regles d'habilitation — permissions,
 * organigramme, competence territoriale. Le moteur ignore volontairement ces notions.</p>
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret des transitions
 */
@FunctionalInterface
public interface ITransitionCondition<D extends IData, T extends Transition<D>> {

    /**
     * Indique si la transition est franchissable dans le contexte fourni.
     *
     * @param pContexte   contexte d'execution portant la donnee et l'utilisateur
     * @param pTransition transition a evaluer
     * @return vrai si la transition est autorisee, faux sinon
     */
    boolean estAutorise(ExecutionContext<D> pContexte, T pTransition);
}
