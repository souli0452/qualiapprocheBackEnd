package com.qualiapproche.evaluation.enumeration;

/**
 * La note posée sur un critère du référentiel RQAPBF.
 *
 * <p>La grille officielle à quatre niveaux, plus N/A. Chaque niveau porte sa part du score, sur
 * 100 — le barème de la grille est 0 / 0,45 / 0,75 / 1. {@link #NON_APPLICABLE} n'en porte
 * aucune : il sort le critère du calcul au lieu de le compter pour zéro — un critère sans objet
 * pour la structure ne doit pas faire baisser sa note.</p>
 */
public enum NiveauNotationRQAPBF {
    NON_CONFORME(0.0),
    A_AMELIORER(45.0),
    ACCEPTABLE(75.0),
    CONFORME(100.0),
    NON_APPLICABLE(null);

    private final Double score;

    NiveauNotationRQAPBF(Double score) {
        this.score = score;
    }

    /** La part du score, sur 100 ; nulle pour un critère non applicable. */
    public Double getScore() {
        return score;
    }
}
