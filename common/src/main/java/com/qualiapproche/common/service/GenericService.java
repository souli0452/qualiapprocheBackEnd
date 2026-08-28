package com.qualiapproche.common.service;

import com.qualiapproche.common.api.CriteriaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contrat commun des services de ressource, vu de leur contrôleur.
 *
 * <p>Le socle des opérations qu'une ressource offre toutes de la même façon. La première est la
 * recherche : chaque module écrivait la sienne, avec un paramètre par colonne — dix-sept pour les
 * non-conformités — répété dans le contrôleur, le service et la spécification. Les suivantes s'y
 * ajouteront de la même manière, et {@link AbstractController} les exposera sans qu'aucun
 * contrôleur concret n'ait à les réécrire.</p>
 *
 * <p>Séparé de {@link AbstractService} qui le remplit : un service métier hérite de
 * l'implémentation, un contrôleur ne dépend que de ce contrat, et une ressource dont l'une des
 * opérations s'écrit autrement reste exposable par le même point d'entrée.</p>
 *
 * @param <D> objet de transfert rendu à l'appelant
 */
public interface GenericService<D> {

    /**
     * Ressources retenues par les critères, dans les bornes de visibilité de l'appelant.
     *
     * @param criteres critères de l'appelant ; {@code null} vaut « aucun filtre »
     */
    Page<D> rechercher(CriteriaDto criteres, Pageable pageable);
}
