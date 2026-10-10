package com.qualiapproche.referentiel.entities;

/**
 * Un paramétrage qui se retire au lieu de s'effacer.
 *
 * <p>D'autres services en gardent l'identifiant — un audit ses sites, une évaluation son critère
 * et son niveau — sans clé étrangère pour protéger la ligne. L'effacer laisserait ces dossiers
 * pointer dans le vide ; retiré, il reste lisible, et ne se propose simplement plus.</p>
 */
public interface Retirable {

    boolean isActif();

    void setActif(boolean actif);
}
