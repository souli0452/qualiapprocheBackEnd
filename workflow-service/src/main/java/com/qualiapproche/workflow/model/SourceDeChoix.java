package com.qualiapproche.workflow.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Provenance des valeurs d'une liste de choix.
 *
 * <p>Un champ {@code SELECT} proposait jusqu'ici une liste écrite à la main dans le circuit. Cela
 * convient à « Oui, Non » ; cela ne convient pas aux structures, aux utilisateurs ou aux entrées
 * d'un référentiel, qui vivent ailleurs et changent sans qu'on remanie le circuit. Une structure
 * créée après coup n'y aurait jamais figuré, et un libellé corrigé au référentiel serait resté faux
 * dans toutes les décisions à venir.</p>
 *
 * <p>La source s'inscrit dans le champ {@code options}, là où s'inscrirait la liste littérale : un
 * préfixe la distingue, et les circuits existants — dont les options sont des libellés — continuent
 * d'être lus comme avant.</p>
 */
public enum SourceDeChoix {

    /** Structures du référentiel. La valeur retenue est l'identifiant de la structure. */
    STRUCTURES("@STRUCTURES"),

    /** Utilisateurs de la plateforme. La valeur retenue est l'identifiant de l'utilisateur. */
    UTILISATEURS("@UTILISATEURS"),

    /**
     * Utilisateurs de la structure de celui qui décide.
     *
     * <p>Un pilote impute un dossier à quelqu'un de son équipe, pas à n'importe quel agent de la
     * plateforme. Lui présenter l'annuaire entier, c'est le laisser désigner une personne qui ne
     * relève pas de lui — et rendre la liste impraticable dès que l'organisation grandit.</p>
     *
     * <p>La structure n'est pas inscrite dans le circuit : elle est celle de l'appelant, résolue à
     * l'affichage. Le même circuit sert donc toutes les structures.</p>
     */
    UTILISATEURS_DE_MA_STRUCTURE("@UTILISATEURS_MA_STRUCTURE"),

    /**
     * Circuits de traitement d'une non-conformité — action corrective ou correction.
     *
     * <p>Une source plutôt qu'une liste littérale, bien que ces deux valeurs ne changent jamais :
     * une liste écrite dans le circuit aurait fait retenir le <b>libellé</b> comme valeur, et le
     * module métier aurait eu à reconnaître « Action corrective » pour en tirer une constante.
     * Corriger l'orthographe du libellé dans l'éditeur aurait alors suffi à faire perdre le
     * choix.</p>
     */
    CIRCUITS_TRAITEMENT("@CIRCUITS_TRAITEMENT");

    private final String cle;

    SourceDeChoix(String cle) {
        this.cle = cle;
    }

    public String getCle() {
        return cle;
    }

    /**
     * Source désignée par une valeur d'{@code options}, ou vide s'il s'agit d'une liste littérale.
     */
    public static Optional<SourceDeChoix> depuisOptions(String options) {
        if (options == null || options.isBlank()) {
            return Optional.empty();
        }
        String valeur = options.trim();
        return Arrays.stream(values())
                .filter(source -> source.cle.equalsIgnoreCase(valeur))
                .findFirst();
    }
}
