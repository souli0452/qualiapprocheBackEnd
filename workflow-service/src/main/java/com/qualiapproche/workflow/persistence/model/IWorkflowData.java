package com.qualiapproche.workflow.persistence.model;

import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.model.Etat;

import java.util.UUID;

/**
 * Contrat d'une donnée persistée pilotée par le moteur de workflow.
 */
public interface IWorkflowData extends IData {

    /**
     * @return L'identifiant unique de la donnée.
     */
    UUID getId();

    /**
     * @return Le code du workflow auquel cette donnée est rattachée.
     */
    String getWorkflowCode();

    /**
     * @param workflowCode Le code du workflow.
     */
    void setWorkflowCode(String workflowCode);

    /**
     * @return L'observation courante.
     */
    String getObservation();

    /**
     * @param observation L'observation à définir.
     */
    void setObservation(String observation);

    /**
     * Retourne le code de l'état persisté en base de données.
     * Cela permet de recharger l'état sans nécessiter le moteur.
     *
     * @return le code de l'état
     */
    String getEtatCode();

    /**
     * Applique l'état du moteur à l'entité JPA.
     * Cette méthode doit synchroniser la représentation moteur (objet Etat) 
     * avec la représentation JPA (ex: string etatCode).
     *
     * @param etat l'état à appliquer
     */
    void appliquerEtat(Etat etat);
}
