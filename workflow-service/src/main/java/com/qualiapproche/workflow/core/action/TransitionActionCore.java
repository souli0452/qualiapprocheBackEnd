package com.qualiapproche.workflow.core.action;

import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.interfaces.ITransitionAction;
import com.qualiapproche.workflow.core.model.ActionExecutionContext;
import com.qualiapproche.workflow.core.model.Transition;

/**
 * Squelette d'action : controle, puis {@code before} / {@code update} / {@code after},
 * avec un point de reprise unique sur exception.
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret de la transition
 */
public abstract class TransitionActionCore<D extends IData, T extends Transition<D>>
        implements ITransitionAction<D, T> {

    /**
     * Enchaine le controle du contexte puis les etapes {@code before}, {@code update} et
     * {@code after}, toute exception etant redirigee vers {@code onException}.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si une etape echoue
     */
    @Override
    public final void executerAction(final ActionExecutionContext<D, T> pContexte) throws WorkflowException {
        try {
            pContexte.controle();
            this.before(pContexte);
            this.update(pContexte);
            this.after(pContexte);
        } catch (Exception e) {
            this.onException(pContexte, e);
        }
    }

    /**
     * Preparation : verifications metier, chargement de donnees annexes.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si la preparation echoue
     */
    protected abstract void before(ActionExecutionContext<D, T> pContexte) throws WorkflowException;

    /**
     * Coeur de l'action : c'est ici que l'etat de la donnee est modifie.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si la mise a jour echoue
     */
    protected abstract void update(ActionExecutionContext<D, T> pContexte) throws WorkflowException;

    /**
     * Suites : notifications, journalisation, declenchements annexes.
     *
     * @param pContexte contexte d'execution de l'action
     * @throws WorkflowException si le traitement des suites echoue
     */
    protected abstract void after(ActionExecutionContext<D, T> pContexte) throws WorkflowException;

    /**
     * Traitement des erreurs. Le comportement par defaut propage : une action qui echoue
     * ne doit pas laisser croire que la transition a abouti.
     *
     * <p>Une redefinition qui n'aboutit pas a une levee vaut acquittement silencieux de
     * l'erreur — a n'utiliser que si la transition est reellement consideree comme
     * franchie.</p>
     *
     * @param pContexte  contexte d'execution de l'action
     * @param pException exception levee par l'une des etapes
     * @throws WorkflowException systematiquement, sauf redefinition
     */
    protected void onException(final ActionExecutionContext<D, T> pContexte, final Exception pException)
            throws WorkflowException {
        switch (pException) {
            case WorkflowException aWorkflowException -> throw aWorkflowException;
            case RuntimeException aRuntimeException -> throw aRuntimeException;
            case null -> throw new WorkflowException("Echec sans cause de l'action de la transition "
                    + codeTransition(pContexte) + ".");
            default -> throw new WorkflowException("Echec de l'action de la transition "
                    + codeTransition(pContexte) + ".", pException);
        }
    }

    private String codeTransition(final ActionExecutionContext<D, T> pContexte) {
        return pContexte == null || pContexte.getTransition() == null
                ? "inconnue"
                : pContexte.getTransition().getCode();
    }
}
