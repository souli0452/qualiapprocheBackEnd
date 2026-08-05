package com.qualiapproche.workflow.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

/**
 * Couleur du bouton qui déclenche une transition, exprimée dans le vocabulaire de PrimeNG.
 *
 * <p>L'API échange le jeton tel que {@code <p-button [severity]>} l'attend ({@code "success"},
 * {@code "warn"}…) plutôt que le nom de la constante : le libellé transite jusqu'au composant
 * sans conversion intermédiaire, et la liste des valeurs admises reste vérifiée côté serveur.</p>
 *
 * <p>La lecture est tolérante — casse libre, et {@code "warning"} accepté pour {@code WARN},
 * la sévérité ayant changé de nom entre PrimeNG 17 et 18.</p>
 */
public enum SeveriteAction {

    PRIMARY("primary"),
    SECONDARY("secondary"),
    SUCCESS("success"),
    INFO("info"),
    WARN("warn", "warning"),
    DANGER("danger"),
    HELP("help"),
    CONTRAST("contrast");

    private final String code;
    private final String[] alias;

    SeveriteAction(final String pCode, final String... pAlias) {
        this.code = pCode;
        this.alias = pAlias;
    }

    /** Jeton PrimeNG, seule forme émise par l'API. */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * Sévérité désignée par {@code valeur}, ou {@code null} si rien n'est renseigné.
     *
     * @throws IllegalArgumentException si la valeur ne correspond à aucune sévérité connue
     */
    @JsonCreator
    public static SeveriteAction depuis(final String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String normalisee = valeur.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(s -> s.code.equals(normalisee)
                        || s.name().toLowerCase(Locale.ROOT).equals(normalisee)
                        || Arrays.asList(s.alias).contains(normalisee))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sévérité inconnue : '" + valeur + "'. Valeurs acceptées : " + jetons() + "."));
    }

    /** Jetons admis, pour les messages d'erreur et la documentation de l'API. */
    public static String jetons() {
        return String.join(", ", Arrays.stream(values()).map(SeveriteAction::getCode).toList());
    }
}
