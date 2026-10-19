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
    NON_CONFORME(0.0, "Non conforme"),
    A_AMELIORER(45.0, "À améliorer"),
    ACCEPTABLE(75.0, "Acceptable"),
    CONFORME(100.0, "Conforme"),
    NON_APPLICABLE(null, "Non applicable");

    private final Double score;
    private final String libelle;

    NiveauNotationRQAPBF(Double score, String libelle) {
        this.score = score;
        this.libelle = libelle;
    }

    /** Les niveaux dont une direction choisit s'ils ouvrent une non-conformité (RegleEcartRQAPBF). */
    public static java.util.List<NiveauNotationRQAPBF> parametrables() {
        return java.util.List.of(NON_CONFORME, A_AMELIORER, ACCEPTABLE);
    }

    /** La règle tant qu'une direction n'en a pas posé d'autre : « Acceptable » n'est pas un écart. */
    public boolean ouvreUneNonConformiteParDefaut() {
        return this == NON_CONFORME || this == A_AMELIORER;
    }

    public String getLibelle() {
        return libelle;
    }

    /** La part du score, sur 100 ; nulle pour un critère non applicable. */
    public Double getScore() {
        return score;
    }
}
