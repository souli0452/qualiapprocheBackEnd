package com.qualiapproche.gateway;

import java.time.Duration;

/**
 * Un seau de jetons : {@code capacite} jetons au plus, qui se remplit d'un jeton toutes les
 * {@code periode / capacite}.
 *
 * <p>Préféré à un compteur par minute calendaire, qui laisse passer le double du seuil à cheval
 * sur deux minutes : le seau autorise une rafale de {@code capacite} essais, puis un rythme
 * régulier — exactement ce qu'on attend d'un utilisateur qui se trompe de mot de passe, et
 * exactement ce qui ruine un essai de mots de passe en série.</p>
 *
 * <p>Le temps est passé en paramètre (nanosecondes d'une horloge monotone) : le seau se teste sans
 * attendre.</p>
 */
final class SeauDeJetons {

    private final int capacite;
    /** Nanosecondes pour regagner un jeton. */
    private final double nanosParJeton;

    private double jetons;
    private long dernierRemplissage;

    SeauDeJetons(int capacite, Duration periode, long maintenant) {
        this.capacite = capacite;
        this.nanosParJeton = (double) periode.toNanos() / capacite;
        this.jetons = capacite;
        this.dernierRemplissage = maintenant;
    }

    /**
     * Prend un jeton s'il y en a un.
     *
     * @return {@code 0} si le jeton a été pris ; sinon le nombre de secondes (au moins 1) avant
     *         qu'un jeton soit de nouveau disponible — la valeur de {@code Retry-After}.
     */
    synchronized long prendre(long maintenant) {
        remplir(maintenant);
        if (jetons >= 1) {
            jetons -= 1;
            return 0;
        }
        double nanosManquantes = (1 - jetons) * nanosParJeton;
        return Math.max(1, (long) Math.ceil(nanosManquantes / 1_000_000_000d));
    }

    private void remplir(long maintenant) {
        long ecoule = maintenant - dernierRemplissage;
        if (ecoule > 0) {
            jetons = Math.min(capacite, jetons + ecoule / nanosParJeton);
            dernierRemplissage = maintenant;
        }
    }
}
