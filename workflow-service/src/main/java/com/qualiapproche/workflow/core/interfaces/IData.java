package com.qualiapproche.workflow.core.interfaces;

import java.io.Serializable;

import com.qualiapproche.workflow.core.model.Etat;

/**
 * Contrat minimal que doit remplir toute donnee pilotee par un workflow.
 *
 * <p>Volontairement reduit a l'etat : tout attribut metier (porteur, dates, pieces
 * jointes, montants) appartient a l'implementation, pas au moteur.</p>
 *
 * <p>L'extension de {@link Serializable} rend coherente la serialisation de
 * {@link com.qualiapproche.workflow.core.model.ExecutionContext} : le contexte se declare
 * serialisable, or il porte la donnee. Sans cette contrainte, sa serialisation
 * echouait a l'execution des que la donnee ne l'etait pas.</p>
 */
public interface IData extends Serializable {

    /**
     * Retourne l'etat courant de la donnee.
     *
     * @return l'etat courant de la donnee
     */
    Etat getEtat();

    /**
     * Positionne l'etat courant de la donnee.
     *
     * @param pEtat nouvel etat de la donnee
     */
    void setEtat(Etat pEtat);

    /**
     * Retourne le code du workflow auquel appartient cette donnée.
     * Permet de gérer plusieurs workflows pour une même classe de donnée.
     *
     * @return le code du workflow
     */
    default String getWorkflowCode() {
        return null;
    }
}
