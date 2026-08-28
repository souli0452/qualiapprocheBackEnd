package com.qualiapproche.workflow.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Personnes qui co-signent une étape, telles qu'elles sont stockées : des identifiants
 * d'utilisateur séparés par des virgules.
 *
 * <p>Des <b>personnes</b>, et non des rôles : le rôle responsable de l'étape dit déjà qui peut y
 * décider, et le redire ne servirait à rien. Ce que cette liste ajoute, c'est <b>qui</b> engage sa
 * signature ici — nommément. Celui d'entre eux qui a soumis le dossier ne le décide plus à cette
 * étape : on ne vérifie pas son propre document.</p>
 *
 * <p>La lecture et l'écriture passent par ici, et non par des {@code split(",")} disséminés :
 * l'espacement et les valeurs vides s'y traitent une seule fois. La casse, elle, est conservée à
 * l'écriture — c'est ce que l'éditeur a envoyé, et il doit le retrouver tel quel pour rapprocher
 * chaque identifiant de la personne qu'il affiche. Seule la <b>comparaison</b> l'ignore, par
 * {@link #designe(Set, String)} : deux graphies d'un même identifiant désignent la même personne,
 * et laisser une majuscule décider de qui peut signer serait indéfendable.</p>
 */
public final class Cosignataires {

    private static final String SEPARATEUR = ",";

    private Cosignataires() {
    }

    /** Identifiants portés par la chaîne, sans espaces superflus ni doublon, dans l'ordre d'écriture. */
    public static Set<String> lire(String cosignataires) {
        if (cosignataires == null || cosignataires.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(cosignataires.split(SEPARATEUR))
                .map(String::trim)
                .filter(utilisateur -> !utilisateur.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Forme stockable d'une liste d'identifiants.
     *
     * <p>Rend {@code null} plutôt qu'une chaîne vide : une étape qui ne nomme personne n'active pas
     * la règle, et la colonne doit le dire — une chaîne vide se relirait de la même façon, mais
     * laisserait croire à une liste effacée là où il n'y en a jamais eu.</p>
     */
    public static String ecrire(Collection<String> cosignataires) {
        if (cosignataires == null || cosignataires.isEmpty()) {
            return null;
        }
        String ecrit = cosignataires.stream()
                .filter(utilisateur -> utilisateur != null && !utilisateur.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(SEPARATEUR));
        return ecrit.isEmpty() ? null : ecrit;
    }

    /**
     * Cette personne compte-t-elle parmi les co-signataires de l'étape ?
     *
     * <p>Comparaison insensible à la casse : les identifiants viennent de Keycloak d'un côté — le
     * sujet du jeton, inscrit sur le dossier à son ouverture — et de l'écran de configuration de
     * l'autre. Rien ne garantit que les deux chaînes traversent la même normalisation, et un
     * contrôle de séparation des signatures qui se laisserait défaire par une majuscule ne vaudrait
     * pas grand-chose.</p>
     */
    public static boolean designe(Set<String> cosignataires, String utilisateur) {
        if (cosignataires == null || cosignataires.isEmpty() || utilisateur == null || utilisateur.isBlank()) {
            return false;
        }
        return cosignataires.stream().anyMatch(utilisateur.trim()::equalsIgnoreCase);
    }

    /**
     * L'appelant est-il écarté de l'étape parce qu'il en est à la fois le signataire et l'auteur ?
     *
     * <p>Les trois termes de la règle, réunis en un seul endroit : le contrôle d'habilitation la
     * pose pour <b>refuser</b> la décision, et l'état publié à l'écran pour l'<b>expliquer</b> —
     * sans quoi l'écran répondrait « le pilote doit se prononcer » à un pilote qui l'est.</p>
     *
     * <p>Ne dit rien du privilège d'administration, qui passe outre : le contrôle d'habilitation
     * s'en charge, et l'écran n'a pas à le savoir puisqu'il propose alors les actions.</p>
     */
    public static boolean ecarteLAuteur(Set<String> cosignataires, String createur, String appelant) {
        if (createur == null || createur.isBlank() || appelant == null || !createur.equals(appelant)) {
            return false;
        }
        return designe(cosignataires, createur);
    }
}
