package com.qualiapproche.common.utils;

import java.util.Set;

/**
 * Rôles dont la portée dépasse une structure.
 *
 * <p>Deux fonctions couvrent la plateforme entière : l'administration, et la responsabilité
 * qualité. L'une et l'autre voient les dossiers de toutes les structures et peuvent intervenir à
 * n'importe quelle étape d'un circuit — c'est le sens même de leur rôle : débloquer ce que
 * l'organisation ordinaire ne débloque pas.</p>
 *
 * <p>Énoncés ici plutôt que dans chaque service : la liste était portée par une constante privée du
 * moteur de workflow, invisible des modules métier, qui composaient donc leurs listes sans la
 * connaître. Un responsable qualité pouvait décider sur un dossier qu'aucun de ses écrans ne lui
 * montrait.</p>
 */
public final class RolesPlateforme {

    /** Administration technique de la plateforme. */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** Responsabilité du système qualité, transverse aux structures. */
    public static final String RESPONSABLE_QUALITE = "RESPONSABLE_QUALITE";

    /**
     * Rôles qui <b>voient</b> tous les dossiers, quelle que soit la structure.
     *
     * <p>Voir n'est pas décider. Le responsable qualité a la vue d'ensemble du système — c'est sa
     * fonction — mais il n'agit qu'aux étapes qui lui sont confiées, comme chacun. Confondre les
     * deux revenait à lui ouvrir toutes les décisions de tous les dossiers, ce qui vide de son sens
     * l'habilitation portée par les étapes.</p>
     */
    public static final Set<String> PORTEE_GLOBALE = Set.of(SUPER_ADMIN, RESPONSABLE_QUALITE);

    /**
     * Rôles qui peuvent <b>décider</b> à n'importe quelle étape, quelle que soit son habilitation.
     *
     * <p>L'administration seule : c'est le moyen de débloquer un dossier dont l'étape courante
     * n'attribue de décision à personne de disponible — titulaire parti, rôle sans porteur. Un
     * privilège d'exception, non un rôle métier.</p>
     */
    public static final Set<String> DECIDE_PARTOUT = Set.of(SUPER_ADMIN);

    /**
     * Habilitation qui ne désigne pas un rôle, mais la personne à qui le dossier a été confié.
     *
     * <p>Certaines étapes ne se décident pas par appartenance à un groupe : l'agent à qui une
     * non-conformité est imputée traite <b>la sienne</b>. Nommer cela « agent imputé » et en faire
     * un rôle ouvrait l'étape à tous ceux qui peuvent être imputés, sur tous les dossiers.</p>
     *
     * <p>Le préfixe la distingue d'un nom de rôle, qu'aucun ne peut porter. Elle s'écrit là où
     * s'écrirait un rôle — habilitation d'une transition, rôle responsable d'une étape ou de son
     * modèle au catalogue — de sorte que la chaîne de résolution existante la transporte sans
     * traitement particulier.</p>
     */
    public static final String HABILITATION_TITULAIRE = "@TITULAIRE";

    private RolesPlateforme() {
    }
}
