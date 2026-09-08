package com.qualiapproche.common.utils;

/**
 * Habilitations dont la portée dépasse une structure.
 *
 * <p>Elles remplacent les listes de noms de rôles qui portaient jusqu'ici ces deux privilèges.
 * Un nom de rôle n'est pas une habilitation : les rôles se créent depuis l'écran d'administration,
 * chaque installation nomme les siens, et le code qui teste {@code "RESPONSABLE_QUALITE"} ignore
 * tout d'un rôle « Coordonnateur qualité » créé le lendemain — lequel se voit refuser un droit que
 * sa fonction suppose, sans que rien ne l'explique. La permission, elle, s'accorde à n'importe
 * quel rôle depuis l'écran, et le code n'a pas à connaître son nom.</p>
 *
 * <p>Deux privilèges distincts, et il ne faut pas les confondre — les tenir pour un seul revenait
 * à ouvrir toutes les décisions de tous les dossiers à qui n'avait besoin que de les lire :</p>
 * <ul>
 *   <li><b>Voir</b> les dossiers de toutes les structures ;</li>
 *   <li><b>Décider</b> à n'importe quelle étape, quelle que soit son habilitation.</li>
 * </ul>
 *
 * <p>Ce sont des constantes de compilation : elles s'écrivent donc aussi dans les annotations
 * {@code @PreAuthorize}, qui n'acceptent que des expressions constantes.</p>
 */
public final class PermissionsPortee {

    /**
     * Voir les dossiers de toutes les structures.
     *
     * <p>Voir n'est pas décider. Cette permission ouvre la lecture transverse — tableaux de bord de
     * l'organisme, dossiers des autres structures — et rien de plus : son porteur n'agit qu'aux
     * étapes qui lui sont confiées, comme chacun.</p>
     */
    public static final String TOUTES_STRUCTURES = "portee-toutes-structures";

    /**
     * Décider à n'importe quelle étape, quelle que soit l'habilitation qu'elle exige.
     *
     * <p>Le moyen de débloquer un dossier dont l'étape courante n'attribue de décision à personne
     * de disponible — titulaire parti, rôle sans porteur. Un privilège d'exception, à n'accorder
     * qu'à l'administration : posé sur un rôle métier, il vide de son sens l'habilitation portée
     * par les étapes.</p>
     */
    public static final String DECIDER_PARTOUT = "circuit-decider-partout";

    /**
     * Voir les documents quel que soit leur niveau de confidentialité.
     *
     * <p>Dispense du classement, et d'elle seule : son porteur voit un document que son niveau
     * réserve à d'autres. Elle existe pour qu'un document mal classé — sur un rôle que plus
     * personne ne détient — reste réparable ; sans elle, plus personne ne pourrait ni le voir ni
     * le reclasser.</p>
     *
     * <p>Distincte de {@link #TOUTES_STRUCTURES} : le responsable qualité voit toutes les
     * structures, pas tous les classements.</p>
     */
    public static final String HORS_CLASSEMENT = "document-hors-classement";

    private PermissionsPortee() {
    }
}
