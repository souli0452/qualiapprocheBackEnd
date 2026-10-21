package com.qualiapproche.common.serie;

/**
 * Ce qu'un type de document dit de sa numérotation : dans quelle série il puise, d'où elle part,
 * et comment un rang devient un numéro.
 *
 * @param <C> ce dont la stratégie a besoin pour décider (structure, année, sigle…)
 */
public interface StrategieDeSerie<C> {

    /** La série où puiser : sa clé encode la portée d'unicité. */
    String cle(C contexte);

    /**
     * La valeur d'une série neuve : le plus grand rang déjà attribué dans cette portée, 0 s'il n'y
     * en a pas. Une série créée après coup reprend ainsi la suite des numéros existants.
     */
    long valeurInitiale(C contexte);

    /** Le numéro lisible, pour ce rang. */
    String formater(C contexte, long rang);

    /** Le plus grand rang admis : au-delà, la série est épuisée et l'attribution refusée. */
    default long rangMaximal() {
        return Long.MAX_VALUE;
    }
}
