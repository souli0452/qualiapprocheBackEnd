package com.qualiapproche.workflow.core.model;

import lombok.Getter;
import lombok.Setter;
import com.qualiapproche.workflow.core.exception.ConfigurationWorkflowException;
import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.interfaces.ITransitionAction;

import java.io.Serializable;
import java.util.Objects;

/**
 * Arc oriente entre deux etats, porteur de l'action a executer lors du franchissement.
 *
 * <p>Correction majeure par rapport a l'implementation historique : {@code equals} et
 * {@code hashCode} sont definis. La classe est stockee dans des {@code HashSet} et
 * indexee par etat ; sans eux, deux chargements successifs du workflow produisaient des
 * transitions jamais egales entre elles.</p>
 *
 * @param <D> type de la donnee pilotee par le workflow
 */
@Getter
@Setter
public class Transition<D extends IData> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String libelle;
    private String description;
    private Etat etatOrigine;
    private Etat etatDestination;

    /**
     * -- GETTER --
     * Action configuree pour la transition.
     *
     * @return l'action configuree, ou null si aucune action n'est posee
     */
    private transient ITransitionAction<D, ?> action;

    /**
     * Construit une transition vide.
     */
    public Transition() {
        // constructeur par defaut requis par les couches de mapping
    }

    /**
     * Construit une transition entre deux etats.
     *
     * @param pCode            code de la transition
     * @param pEtatOrigine     etat d'origine
     * @param pEtatDestination etat de destination
     */
    public Transition(final String pCode, final Etat pEtatOrigine, final Etat pEtatDestination) {
        this.code = pCode;
        this.etatOrigine = pEtatOrigine;
        this.etatDestination = pEtatDestination;
    }

    /**
     * Franchit la transition en deleguant a l'action configuree.
     *
     * @param pContexte contexte d'execution de l'appel
     * @throws WorkflowException si aucune action n'est configuree ou si l'action echoue
     */
    @SuppressWarnings("unchecked")
    public final void franchir(final ExecutionContext<D> pContexte) throws WorkflowException {
        if (this.action == null) {
            throw new ConfigurationWorkflowException(
                    "Aucune action n'est configuree pour la transition " + this.code + ".");
        }
        ITransitionAction<D, Transition<D>> aAction = (ITransitionAction<D, Transition<D>>) this.action;
        aAction.executerAction(new ActionExecutionContext<>(pContexte, this));
    }

    /**
     * Affecte l'action a executer. Le parametre {@code T} lie l'action au type reel de
     * la transition : une action ecrite pour un sous-type ne peut pas etre posee sur une
     * transition d'un autre type.
     *
     * @param <T>     type concret de la transition attendu par l'action
     * @param pAction action a executer lors du franchissement
     */
    public <T extends Transition<D>> void setAction(final ITransitionAction<D, T> pAction) {
        this.action = pAction;
    }

    /**
     * Empreinte calculee sur le seul code.
     *
     * @return l'empreinte de la transition
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.code);
    }

    /**
     * Egalite par code, tolerante aux sous-classes.
     *
     * @param pObj objet compare
     * @return vrai si l'objet compare est une transition de meme code
     */
    @Override
    public boolean equals(final Object pObj) {
        return this == pObj
                || pObj instanceof Transition<?> aAutre && Objects.equals(this.code, aAutre.code);
    }

    /**
     * Representation lisible de la transition.
     *
     * @return le code, l'etat d'origine et l'etat de destination
     */
    @Override
    public String toString() {
        return "Transition [code=%s, origine=%s, destination=%s]"
                .formatted(this.code, this.etatOrigine, this.etatDestination);
    }
}
