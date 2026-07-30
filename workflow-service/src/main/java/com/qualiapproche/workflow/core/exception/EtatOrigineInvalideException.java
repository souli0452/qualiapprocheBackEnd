package com.qualiapproche.workflow.core.exception;

import lombok.Getter;
import com.qualiapproche.workflow.core.model.Etat;

/**
 * Levee lorsque l'etat courant de la donnee ne correspond pas a l'etat d'origine
 * de la transition demandee. C'est la garde centrale du moteur.
 */
public class EtatOrigineInvalideException extends WorkflowException {

    private static final long serialVersionUID = 1L;

    @Getter private final transient Etat etatCourant;
    @Getter private final transient Etat etatAttendu;

    /**
     * Construit l'exception a partir de l'etat courant et de l'etat attendu.
     *
     * @param pEtatCourant etat courant de la donnee
     * @param pEtatAttendu etat d'origine attendu par la transition
     */
    public EtatOrigineInvalideException(final Etat pEtatCourant, final Etat pEtatAttendu) {
        super("L'etat de la donnee (" + libelle(pEtatCourant) + ") est different de l'etat d'origine"
                + " de la transition (" + libelle(pEtatAttendu) + ").");
        this.etatCourant = pEtatCourant;
        this.etatAttendu = pEtatAttendu;
    }

    private static String libelle(final Etat pEtat) {
        return pEtat == null ? "null" : pEtat.getCode();
    }

}
