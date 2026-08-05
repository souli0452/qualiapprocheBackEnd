package com.qualiapproche.common.utils;

/**
 * États d'avancement d'une action corrective.
 *
 * <p>Une action n'est {@code TRAITER} qu'une fois son efficacité reconnue, et non dès que son
 * responsable la déclare faite : c'est ce statut qui autorise la clôture de la non-conformité, et
 * la déclarer soldée sur la seule parole de celui qui l'a menée revenait à clore un dossier sans
 * qu'aucun effet n'ait été constaté.</p>
 */
public enum StatutEnum {
    ACTIF,
    INACTIF,
    /** Confiée à son responsable, pas encore menée. */
    NON_TRAITER,
    /** Déclarée réalisée par son responsable, en attente du constat du pilote. */
    EN_VERIFICATION,
    /** Réalisation constatée ; reste à confronter le résultat au critère d'efficacité. */
    EFFICACITE_A_MESURER,
    /** Réalisée et reconnue efficace : elle ne retient plus la clôture du dossier. */
    TRAITER,
    /** Attribution déclinée : l'action attend d'être confiée à quelqu'un d'autre. */
    REJECTED
}
