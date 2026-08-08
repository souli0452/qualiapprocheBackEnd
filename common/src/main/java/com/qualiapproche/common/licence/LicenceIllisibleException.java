package com.qualiapproche.common.licence;

/**
 * Le texte fourni n'est pas une licence authentique : format inconnu, signature invalide, contenu
 * illisible.
 *
 * <p>À distinguer d'une licence expirée ou destinée à un autre partenaire — celles-là sont
 * authentiques, et méritent un message qui dit la vraie raison du refus. Confondre les deux
 * enverrait l'administrateur vérifier son copier-coller alors que son abonnement a simplement pris
 * fin.</p>
 */
public class LicenceIllisibleException extends RuntimeException {

    public LicenceIllisibleException(String message) {
        super(message);
    }

    public LicenceIllisibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
