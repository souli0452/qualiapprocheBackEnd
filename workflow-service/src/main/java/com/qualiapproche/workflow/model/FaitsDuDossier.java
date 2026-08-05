package com.qualiapproche.workflow.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Faits établis sur un dossier, tels qu'ils sont stockés : des chaînes séparées par des virgules.
 *
 * <p>Le moteur ne sait pas ce qu'est un plan d'action soldé ni une efficacité mesurée. Il sait
 * qu'un dossier « porte » ou « ne porte pas » un fait, et qu'une transition peut en exiger un. Le
 * module métier, lui, sait quand le fait devient vrai — et le déclare. Aucun des deux n'a besoin de
 * connaître l'autre.</p>
 *
 * <p>La lecture et l'écriture passent par ici, et non par des {@code split(",")} disséminés :
 * l'espacement, la casse et les valeurs vides s'y traitent une seule fois.</p>
 */
public final class FaitsDuDossier {

    private static final String SEPARATEUR = ",";

    private FaitsDuDossier() {
    }

    /** Faits portés par la chaîne, normalisés en majuscules et sans doublon. */
    public static Set<String> lire(String faits) {
        if (faits == null || faits.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(faits.split(SEPARATEUR))
                .map(String::trim)
                .filter(fait -> !fait.isEmpty())
                .map(fait -> fait.toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Le dossier porte-t-il ce fait ? */
    public static boolean contient(String faits, String fait) {
        if (fait == null || fait.isBlank()) {
            return true;
        }
        return lire(faits).contains(fait.trim().toUpperCase());
    }

    /**
     * Forme stockable d'un ensemble de faits.
     *
     * <p>Rend {@code null} plutôt qu'une chaîne vide : un dossier sans fait n'en porte aucun, et la
     * colonne doit le dire.</p>
     */
    public static String ecrire(Set<String> faits) {
        if (faits == null || faits.isEmpty()) {
            return null;
        }
        return faits.stream()
                .filter(fait -> fait != null && !fait.isBlank())
                .map(fait -> fait.trim().toUpperCase())
                .distinct()
                .collect(Collectors.joining(SEPARATEUR));
    }
}
