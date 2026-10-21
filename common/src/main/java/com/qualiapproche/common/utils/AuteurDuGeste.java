package com.qualiapproche.common.utils;

import java.util.function.Supplier;

/**
 * L'auteur d'un geste exécuté hors de sa requête — typiquement un rappel du circuit, traité par un
 * écouteur de la messagerie, sans jeton.
 *
 * <p>Sert à l'<b>attribution</b> seulement (« modifié par », journaux d'audit) : il ne confère
 * aucun droit, et {@link SecurityUtils#getCurrentUserId()} l'ignore. Ainsi un dossier mis à jour
 * par un rappel porte le nom de la personne qui a décidé dans le circuit, au lieu de rester vide
 * (ou, du temps des webhooks HTTP, de porter le compte de service).</p>
 */
public final class AuteurDuGeste {

    private record Auteur(String id, String nom) {
    }

    private static final ThreadLocal<Auteur> COURANT = new ThreadLocal<>();

    private AuteurDuGeste() {
    }

    /** Exécute {@code geste} au nom de l'auteur donné ; sans identifiant, l'exécute tel quel. */
    public static <T> T pour(String id, String nom, Supplier<T> geste) {
        if (id == null || id.isBlank()) {
            return geste.get();
        }
        Auteur precedent = COURANT.get();
        COURANT.set(new Auteur(id, nom));
        try {
            return geste.get();
        } finally {
            if (precedent == null) {
                COURANT.remove();
            } else {
                COURANT.set(precedent);
            }
        }
    }

    /** Variante sans résultat de {@link #pour(String, String, Supplier)}. */
    public static void pour(String id, String nom, Runnable geste) {
        pour(id, nom, () -> {
            geste.run();
            return null;
        });
    }

    static String id() {
        Auteur auteur = COURANT.get();
        return auteur == null ? null : auteur.id();
    }

    static String nom() {
        Auteur auteur = COURANT.get();
        return auteur == null ? null : auteur.nom();
    }
}
