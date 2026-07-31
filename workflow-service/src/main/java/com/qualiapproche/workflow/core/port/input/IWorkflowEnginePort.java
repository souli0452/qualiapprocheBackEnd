package com.qualiapproche.workflow.core.port.input;

import java.util.SequencedMap;
import java.util.SequencedSet;

import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.core.model.Workflow;
import com.qualiapproche.workflow.core.model.WorkflowConfig;

/**
 * Port entrant : l'API du moteur, telle que l'application hote la consomme.
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret des transitions
 * @param <W> type concret du workflow
 */
public interface IWorkflowEnginePort<D extends IData, T extends Transition<D>, W extends Workflow<D, T>> {

    /**
     * Charge workflows et configurations depuis le port DAO. A appeler au demarrage.
     *
     * @throws WorkflowException si le chargement des workflows ou des configurations echoue
     */
    void init() throws WorkflowException;

    /**
     * Positionne la donnee sur l'etat initial de son workflow.
     *
     * @param pData donnee a initialiser
     * @return la donnee positionnee sur l'etat initial
     * @throws WorkflowException si aucune configuration n'est trouvee pour la donnee
     */
    D initEtatData(D pData) throws WorkflowException;

    /**
     * Transitions franchissables depuis l'etat courant, filtrees par le port de condition.
     *
     * <p>Le type de retour {@code SequencedSet} garantit l'ordre de declaration des
     * transitions dans le workflow : une IHM peut construire ses boutons d'action
     * directement a partir de cette vue, sans retrier.</p>
     *
     * @param pContexte contexte d'execution portant la donnee et l'utilisateur
     * @return l'ensemble ordonne des transitions franchissables
     * @throws WorkflowException si la configuration du workflow est introuvable ou invalide
     */
    SequencedSet<T> getTransitionsPossibles(ExecutionContext<D> pContexte) throws WorkflowException;

    /**
     * Franchit la transition apres validation de l'etat d'origine.
     *
     * @param pContexte   contexte d'execution portant la donnee et l'utilisateur
     * @param pTransition transition a franchir
     * @throws WorkflowException si l'etat d'origine ne correspond pas ou si la configuration est invalide
     */
    void executerTransition(ExecutionContext<D> pContexte, T pTransition) throws WorkflowException;

    /**
     * Recherche une transition par son code, dans le workflow de la donnee.
     *
     * @param pData            donnee pilotee dont on cherche la transition
     * @param pCodeTransition  code de la transition recherchee
     * @return la transition correspondant au code, ou null si aucune ne correspond
     * @throws WorkflowException si la configuration du workflow de la donnee est invalide
     */
    T getTransitionByCode(D pData, String pCodeTransition) throws WorkflowException;

    /**
     * Recherche un workflow par son code.
     *
     * @param pCodeWorkflow code du workflow recherche
     * @return le workflow correspondant au code, ou null si aucun ne correspond
     * @throws WorkflowException si la recherche du workflow echoue
     */
    W getWorkflowByCode(String pCodeWorkflow) throws WorkflowException;

    /**
     * Catalogue des workflows charges, indexes par code, dans l'ordre de chargement.
     *
     * @return la table ordonnee des workflows indexes par code
     */
    SequencedMap<String, W> getWorkflow();

    /**
     * Configurations chargees, indexees par classe de donnee, dans l'ordre de chargement.
     *
     * @return la table ordonnee des configurations indexees par classe de donnee
     */
    SequencedMap<String, WorkflowConfig<D, T, W>> getWorkflowConfig();
}
