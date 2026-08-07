package com.qualiapproche.common.utils;

/**
 * Clés des réglages de l'organisation que le code lit.
 *
 * <p>Un réglage est désigné par sa clé, et rien ne signale une clé mal orthographiée : la lecture
 * rend simplement du vide, et le pied de page part sans téléphone, ou le courriel sans copie. Les
 * services qui sèment ces réglages et ceux qui les lisent citent donc la même constante.</p>
 *
 * <p>Seules les clés utilisées par du code figurent ici. Celles que l'organisation ajoute depuis
 * l'écran de configuration n'ont pas à y être : personne ne les lit dans le code.</p>
 */
public final class ClesReglages {

    /** Nom de l'organisation, au bas des courriels. */
    public static final String ORGANISATION_NOM = "ORGANISATION_NOM";

    /** Adresse de réponse proposée au bas des courriels. */
    public static final String CONTACT_EMAIL = "CONTACT_EMAIL";

    /** Téléphone affiché au bas des courriels. */
    public static final String CONTACT_TELEPHONE = "CONTACT_TELEPHONE";

    /** Adresse postale affichée au bas des courriels. */
    public static final String ADRESSE_POSTALE = "ADRESSE_POSTALE";

    /** Site institutionnel, en lien au bas des courriels. */
    public static final String SITE_WEB = "SITE_WEB";

    /** Adresse de l'image du logo, affichée au bas des courriels. */
    public static final String LOGO_URL = "LOGO_URL";

    /** Nom complet du responsable qualité. */
    public static final String RESPONSABLE_QUALITE_NOM = "RESPONSABLE_QUALITE_NOM";

    /** Courriel du responsable qualité, mis en copie des imputations de non-conformité. */
    public static final String RESPONSABLE_QUALITE_EMAIL = "RESPONSABLE_QUALITE_EMAIL";

    /** Nombre de jours avant échéance à partir duquel un rappel de plan d'action est envoyé. */
    public static final String RAPPEL_ECHEANCE_JOURS = "RAPPEL_ECHEANCE_JOURS";

    private ClesReglages() {
    }
}
