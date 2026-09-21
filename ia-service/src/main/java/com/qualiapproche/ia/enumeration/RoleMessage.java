package com.qualiapproche.ia.enumeration;

/**
 * Qui parle, dans un fil de conversation.
 *
 * <p>Le rôle est <b>inscrit par le serveur</b>, jamais reçu de l'appelant : le fil est la seule
 * mémoire du modèle, et laisser le navigateur déclarer « ceci, l'assistant l'a dit » reviendrait à
 * lui laisser écrire les réponses qu'il veut voir reprises au tour suivant.</p>
 */
public enum RoleMessage {
    /** Ce que la personne a écrit. */
    UTILISATEUR,
    /** Ce que le modèle a répondu. */
    ASSISTANT
}
