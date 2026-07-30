package com.qualiapproche.workflow.core.model;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import com.qualiapproche.workflow.core.interfaces.IData;

/**
 * Contexte enrichi remis a l'action : donnee, parametres, et la transition franchie.
 *
 * <p>Le parametre {@code T} evite aux actions de transtyper pour atteindre les attributs
 * propres a leur type de transition.</p>
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret de la transition
 */
public class ActionExecutionContext<D extends IData, T extends Transition<D>> extends ExecutionContext<D> {

    private static final long serialVersionUID = 1L;

    @Getter @Setter private T transition;

    /**
     * Construit un contexte d'action vide.
     */
    public ActionExecutionContext() {
        // constructeur par defaut requis par les couches de mapping
    }

    /**
     * Construit un contexte d'action a partir d'une donnee et d'une transition.
     *
     * @param pData       donnee pilotee
     * @param pTransition transition franchie
     */
    public ActionExecutionContext(final D pData, final T pTransition) {
        super(pData);
        this.transition = pTransition;
    }

    /**
     * Derive un contexte d'action a partir d'un contexte d'execution.
     *
     * <p>Les parametres sont partages avec le contexte source — et non copies — afin
     * qu'une action puisse transmettre un resultat a l'appelant.</p>
     *
     * <p>Le controle du contexte source est porte par l'argument de {@code super} : un
     * contexte null est signale par un message explicite, la ou l'appel
     * {@code super(pContexte.getData())} levait un {@code NullPointerException} muet.</p>
     *
     * @param pContexte   contexte d'execution source
     * @param pTransition transition franchie
     */
    public ActionExecutionContext(final ExecutionContext<D> pContexte, final T pTransition) {
        super(Objects.requireNonNull(pContexte, "Le contexte d'execution source ne peut etre null.").getData(),
                pContexte.getParametresInternes());
        this.transition = pTransition;
    }

    /**
     * Verifie la coherence du contexte d'action avant execution.
     */
    @Override
    public void controle() {
        super.controle();
        if (this.transition == null) {
            throw new IllegalArgumentException("La transition ne peut etre null.");
        }
    }

    /**
     * Representation lisible du contexte d'action.
     *
     * @return la donnee et la transition franchie
     */
    @Override
    public String toString() {
        return "ActionExecutionContext [data=%s, transition=%s]"
                .formatted(this.getData(), this.transition);
    }
}
