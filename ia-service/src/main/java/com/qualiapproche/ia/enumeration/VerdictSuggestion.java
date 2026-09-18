package com.qualiapproche.ia.enumeration;

/**
 * Sort réservé par l'utilisateur à une suggestion de l'assistant.
 *
 * <p>C'est la matière du KPI d'acceptation : la part des suggestions retenues, retouchées ou
 * écartées mesure la pertinence réelle de l'assistance, là où un simple compteur d'appels ne
 * dirait rien.</p>
 */
public enum VerdictSuggestion {
    /** La suggestion a été reprise telle quelle. */
    ACCEPTEE,
    /** La suggestion a été reprise après retouche. */
    MODIFIEE,
    /** La suggestion a été écartée. */
    REJETEE
}
