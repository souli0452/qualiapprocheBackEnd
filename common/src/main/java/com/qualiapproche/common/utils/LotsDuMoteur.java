package com.qualiapproche.common.utils;

/**
 * Taille des lots dans lesquels on interroge le moteur de workflow sur plusieurs dossiers.
 *
 * <p>Les points d'entrée en lot du moteur refusent au-delà : un appelant qui pousse une page
 * entière sans la découper reçoit une erreur, et sa page perd d'un coup les actions de tous ses
 * dossiers. Le nombre était recopié dans chaque appelant, où rien ne le rattachait à la limite
 * qu'il respecte — le relever d'un côté sans l'autre aurait suffi à rouvrir la panne.</p>
 */
public final class LotsDuMoteur {

    /** Doit rester inférieure ou égale à la limite que le moteur applique à ses appels en lot. */
    public static final int TAILLE_MAX = 200;

    private LotsDuMoteur() {
    }
}
