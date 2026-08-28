package com.qualiapproche.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ce sur quoi porte une recherche : un texte libre, et des critères nommés.
 *
 * <p>Un seul objet pour toutes les ressources. Chaque module déclarait jusqu'ici ses propres
 * paramètres de recherche, un par colonne, répétés dans le contrôleur, le service et la
 * spécification — la recherche des non-conformités en comptait dix-sept, positionnels, dont onze
 * chaînes de suite : deux arguments intervertis ne se voyaient ni à la compilation ni à la
 * relecture, et se manifestaient par une recherche qui rend simplement autre chose.</p>
 *
 * <p>Transmis dans le <b>corps</b> de la requête et non dans son URL : une sélection multiple sur
 * plusieurs colonnes dépasse vite ce qu'une chaîne de requête sait porter lisiblement, et une liste
 * d'identifiants n'a pas à être encodée à la main par l'écran.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaDto {

    /**
     * Texte cherché, confronté aux champs que la ressource déclare consultables.
     *
     * <p>Recherche par <b>préfixe</b> : c'est la seule forme qu'un index sait servir, et la seule
     * qui reste tenable quand la table grandit.</p>
     */
    private String search;

    /** Critères nommés, cumulés entre eux par un ET. Vide : la recherche ne restreint rien. */
    @Builder.Default
    private List<FilterExtra> filters = new ArrayList<>();
}
