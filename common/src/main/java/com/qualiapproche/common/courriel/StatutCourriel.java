package com.qualiapproche.common.courriel;

/** Le cycle d'un courriel sortant. */
public enum StatutCourriel {
    /** Déposé, pas encore remis au serveur de messagerie (ou en attente d'une nouvelle tentative). */
    A_ENVOYER,
    /** Revendiqué par une tournée ; redevient reprenable à son échéance si la tournée s'interrompt. */
    EN_COURS,
    /** Remis au serveur de messagerie. */
    ENVOYE,
    /** Tentatives épuisées : à examiner, il ne repartira pas seul. */
    ABANDONNE
}
