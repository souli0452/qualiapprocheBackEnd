package com.qualiapproche.common.api;

/**
 * Comparaisons qu'un critère de recherche peut exprimer.
 *
 * <p>Volontairement fermé : un opérateur est traduit en clause SQL par {@code GenericSpecification},
 * et laisser le client en écrire une reviendrait à lui ouvrir la base. L'énumération est le contrat
 * entre l'écran, qui compose ses filtres, et le serveur, qui seul sait les exécuter.</p>
 */
public enum FilterOperator {

    /** Égalité stricte. Valeur nulle : la colonne doit être nulle. */
    EQ,
    /** Différence. Valeur nulle : la colonne doit être renseignée. */
    NOT_EQ,

    /** Commence par — c'est la forme qui sait se servir d'un index. */
    LIKE,
    /** Contient, n'importe où. */
    CONTAINS,
    /** Se termine par. */
    ENDS_WITH,
    /** Ne contient pas. */
    NOT_CONTAINS,

    /** Appartient à un ensemble de valeurs : la sélection multiple d'un écran. */
    IN,

    /** Supérieur ou égal. */
    GTE,
    /** Inférieur ou égal. */
    LTE,
    /** Strictement supérieur. */
    GT,
    /** Strictement inférieur. */
    LT,

    /** Entre deux bornes, comprises. La seconde est portée par {@code valueTo}. */
    BETWEEN,

    /** La colonne n'est pas renseignée. */
    IS_NULL,
    /** La colonne est renseignée. */
    NOT_NULL
}
