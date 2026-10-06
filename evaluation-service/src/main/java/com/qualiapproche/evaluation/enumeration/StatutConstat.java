package com.qualiapproche.evaluation.enumeration;

/**
 * Les trois paliers d'un constat.
 *
 * <ul>
 *   <li>{@link #BROUILLON} : privé, visible de son seul rédacteur (« Mes constats du jour ») ;</li>
 *   <li>{@link #COMPILE} : versé par son auteur à la compilation d'équipe, visible et encore
 *       modifiable par l'équipe, ou renvoyé en brouillon pour retravail ;</li>
 *   <li>{@link #PUBLIE} : validé en réunion d'équipe, verrouillé ; il alimente le rapport et la
 *       transmission vers les non-conformités.</li>
 * </ul>
 */
public enum StatutConstat {
    BROUILLON,
    COMPILE,
    PUBLIE
}
