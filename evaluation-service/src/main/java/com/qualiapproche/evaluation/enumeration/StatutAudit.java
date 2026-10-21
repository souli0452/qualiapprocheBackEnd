package com.qualiapproche.evaluation.enumeration;

/**
 * Où en est un audit du programme.
 *
 * <p>{@link #EN_RETARD} n'est posé que sur un audit <b>validé mais jamais démarré</b> dont la date
 * de début est passée : il garde ainsi sa seule issue, démarrer ou être annulé. Un audit encore
 * {@link #PLANIFIE} reste planifié même en retard — sa validation manque toujours, et le statut ne
 * doit pas la faire oublier ; un audit {@link #EN_COURS} qui déborde reste en cours. Ces deux
 * retards-là se lisent sur {@code Audit.estEnRetard}, sans changer de statut.</p>
 */
public enum StatutAudit {
    PLANIFIE,
    EN_PREPARATION,
    EN_COURS,
    CLOTURE,
    ANNULE,
    EN_RETARD;

    /** L'audit est-il terminé, d'une façon ou d'une autre ? Plus rien ne s'y modifie. */
    public boolean estTermine() {
        return this == CLOTURE || this == ANNULE;
    }

    /** Ses données de planification se corrigent-elles encore ? Pas une fois démarré. */
    public boolean estModifiable() {
        return this == PLANIFIE || this == EN_PREPARATION || this == EN_RETARD;
    }
}
