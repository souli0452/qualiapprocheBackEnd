package com.qualiapproche.common.messagerie;

/**
 * Les files de la messagerie, et les types logiques de leurs messages : en un seul endroit, pour
 * qu'une faute de frappe ne crée pas une file que personne n'écoute.
 *
 * <p>Toutes commencent par {@code qualisira.} : c'est ce préfixe que les réglages du courtier
 * (livraisons avant rebut, attente croissante) visent.</p>
 */
public final class AdressesMessagerie {

    /** Les courriels à remettre ; workflow-service les consomme et les envoie par SMTP. */
    public static final String COURRIELS = "qualisira.courriels.envoi";

    /** Préfixe des rappels du circuit : un service métier par file. */
    private static final String RAPPELS_DU_CIRCUIT = "qualisira.circuit.rappels.";

    public static final String SERVICE_AMELIORATION = "amelioration-service";
    public static final String SERVICE_SUPPORT = "support-service";

    /** Les rappels du circuit adressés à amelioration-service (non-conformités, plans d'action). */
    public static final String RAPPELS_AMELIORATION = RAPPELS_DU_CIRCUIT + SERVICE_AMELIORATION;
    /** Les rappels du circuit adressés à support-service (documents, demandes). */
    public static final String RAPPELS_SUPPORT = RAPPELS_DU_CIRCUIT + SERVICE_SUPPORT;

    /** Propriété JMS qui porte le type logique du message (et non un nom de classe). */
    public static final String PROPRIETE_TYPE = "_type";
    /** Propriété JMS qui porte l'identifiant de la ligne d'outbox : clé de dédoublonnage du consommateur. */
    public static final String PROPRIETE_IDENTIFIANT = "qualisiraMessageId";

    public static final String TYPE_COURRIEL = "courriel";
    /**
     * Un courriel qui porte un secret (mot de passe temporaire, lien de vérification ou de
     * réinitialisation) : son contenu est effacé de la boîte d'envoi dès sa publication.
     */
    public static final String TYPE_COURRIEL_CONFIDENTIEL = "courriel-confidentiel";
    public static final String TYPE_RAPPEL_CIRCUIT = "rappel-circuit";

    private AdressesMessagerie() {
    }

    /** Vrai si le contenu de ce type de message ne doit pas survivre à sa publication. */
    public static boolean estConfidentiel(String type) {
        return TYPE_COURRIEL_CONFIDENTIEL.equals(type);
    }

    /** La file des rappels pour un type de ressource du circuit, ou {@code null} si aucun service ne le suit. */
    public static String rappelsPour(String typeRessource) {
        if (typeRessource == null) {
            return null;
        }
        return switch (typeRessource.toUpperCase()) {
            case "DOCUMENT", "DEMANDE_DOCUMENT" -> RAPPELS_SUPPORT;
            case "NON_CONFORMITE", "PLAN_ACTION" -> RAPPELS_AMELIORATION;
            default -> null;
        };
    }
}
