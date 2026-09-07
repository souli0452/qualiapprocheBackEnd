package com.qualiapproche.common.utils;

/**
 * Habilitations d'accès aux tableaux de bord, nommées par la <b>portée</b> qu'elles ouvrent.
 *
 * <p>Trois portées, parce qu'il y a trois questions distinctes : ce que je fais, ce que fait ma
 * structure, ce que fait l'organisme. Elles ne nomment pas de rôle — un pilote promu responsable
 * qualité change de rôle, pas de vocabulaire, et une permission dite « du pilote » aurait dû être
 * renommée le jour où un autre rôle en aurait eu besoin.</p>
 *
 * <p>Elles s'ajoutent aux permissions de lecture des dossiers plutôt que de s'y substituer :
 * {@code nc-read} donne accès aux dossiers que l'appelant a le droit de voir, un tableau de bord
 * en rend le <b>dénombrement</b>. Jusqu'ici les deux se confondaient, si bien que tout porteur de
 * {@code nc-read} — donc tout agent — pouvait demander les chiffres de l'organisme entier.</p>
 *
 * <p>Déclarées ici, et non dans chaque module : le nom est écrit à trois endroits — le dictionnaire
 * des permissions, la dotation des rôles, la protection du point d'entrée — et une divergence entre
 * deux d'entre eux ne se manifeste que par un refus d'accès, sans rien qui l'explique.</p>
 *
 * <p>Ce sont des constantes de compilation : elles s'écrivent donc aussi dans les annotations
 * {@code @PreAuthorize}, qui n'acceptent que des expressions constantes.</p>
 */
public final class PermissionsTableauDeBord {

    /** Le tableau de ses propres dossiers : ceux qu'on a déclarés, ceux qui nous sont imputés. */
    public static final String PERSONNEL = "tableau-bord-personnel";

    /** Le tableau d'une structure — la sienne, sauf portée globale. */
    public static final String STRUCTURE = "tableau-bord-structure";

    /** Le tableau de l'organisme entier, toutes structures confondues. */
    public static final String ORGANISME = "tableau-bord-organisme";

    private PermissionsTableauDeBord() {
    }
}
