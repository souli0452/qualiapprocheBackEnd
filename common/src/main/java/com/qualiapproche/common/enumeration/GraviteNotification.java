package com.qualiapproche.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ce qu'une notification réclame de celui qui la reçoit.
 *
 * <p>Le back dit ce que la ligne <b>vaut</b>, non ce à quoi elle ressemble : l'icône et la couleur
 * appartiennent à l'écran, qui seul connaît sa charte. Une gravité inconnue d'un client ancien se
 * lit encore comme du texte, là où un nom de classe CSS ne lui aurait rien dit.</p>
 */
@Schema(description = "Ce qu'une notification réclame de son destinataire. L'écran y branche "
        + "sa couleur et son icône ; le serveur n'en transporte aucune.")
public enum GraviteNotification {

    /** Rien n'est bloqué ni en retard : le dossier attend l'utilisateur, sans plus. */
    INFO,

    /** Une décision est ouverte à l'utilisateur, et le dossier ne progresse pas sans elle. */
    ATTENTION,

    /** Le dossier est en retard, ou son échéance est atteinte. */
    URGENT
}
