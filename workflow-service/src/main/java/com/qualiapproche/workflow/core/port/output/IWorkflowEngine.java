package com.qualiapproche.workflow.core.port.output;

import java.util.List;
import java.util.Map;

import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.core.model.Workflow;
import com.qualiapproche.workflow.core.model.WorkflowConfig;

/**
 * Port sortant : fournit au moteur la definition des workflows et leur configuration.
 *
 * <p>L'implementation decide de la source — base de donnees, fichier, code. Le moteur ne
 * la charge qu'une fois, au demarrage, via {@code init()}.</p>
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret des transitions
 * @param <W> type concret du workflow
 */
public interface IWorkflowEngine<D extends IData, T extends Transition<D>, W extends Workflow<D, T>> {

    /**
     * Retourne l'ensemble des workflows connus de la source.
     *
     * @return la liste des workflows charges
     * @throws WorkflowException si la lecture de la source echoue
     */
    List<W> getAllWorkflow() throws WorkflowException;

    /**
     * Retourne l'ensemble des configurations de workflow connues de la source.
     *
     * @param pWorkflows workflows deja charges, indexes par code, pour que
     *                   l'implementation puisse les rattacher a leur configuration
     * @return la liste des configurations de workflow
     * @throws WorkflowException si la lecture de la source echoue
     */
    List<WorkflowConfig<D, T, W>> getAllWorkflowConfigs(Map<String, W> pWorkflows) throws WorkflowException;

    /**
     * Signature de l'état courant de la source, pour un rechargement conditionnel.
     *
     * <p>Le moteur compare cette signature à celle du catalogue en mémoire : elles
     * diffèrent dès qu'une définition change à la source, ce qui déclenche un rechargement
     * ciblé sans lecture du graphe complet à chaque appel. Une valeur {@code null} signifie
     * que la source ne sait pas se versionner : le moteur s'abstient alors de tout
     * rechargement automatique et conserve son comportement d'origine.</p>
     *
     * @return une signature comparable par {@code equals}, ou {@code null} si indisponible
     * @throws WorkflowException si la lecture de la signature échoue
     */
    default Object getCatalogueVersion() throws WorkflowException {
        return null;
    }
}
