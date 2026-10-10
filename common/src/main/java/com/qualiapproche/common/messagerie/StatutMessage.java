package com.qualiapproche.common.messagerie;

/** Le cycle d'un message dans la boîte d'envoi d'un service. */
public enum StatutMessage {
    /** Déposé, pas encore remis au courtier (ou en attente d'une nouvelle tentative). */
    A_PUBLIER,
    /** Revendiqué par une publication ; redevient reprenable à son échéance si elle s'interrompt. */
    EN_COURS,
    /** Remis au courtier : la suite (relivraison, rebut) relève de lui. */
    PUBLIE
}
