package com.qualiapproche.common.cloisonnement;

/**
 * Le paramétrage par défaut qu'un service pose pour un client : rôles, circuits, modèles de
 * courriel, types de documents…
 *
 * <p>Appelée dans une session déjà placée sous la direction du client ({@link AmorceDesDirections}) :
 * l'implémentation lit et écrit comme si le client était le seul — ses requêtes ne voient que lui,
 * ce qu'elle enregistre lui revient. Elle doit être <b>idempotente</b> : elle est rejouée à la
 * création du client, à chaque démarrage pour les clients connus, et à la demande de
 * l'administrateur de la plateforme. Ce qu'un client a modifié depuis ne doit pas être écrasé.</p>
 */
public interface Amorce {

    /** Pose ou complète le paramétrage par défaut de la direction de la session. */
    void amorcer();

    /** L'ordre de passage entre amorces d'un même service (les plus petits d'abord). */
    default int ordre() {
        return 0;
    }
}
