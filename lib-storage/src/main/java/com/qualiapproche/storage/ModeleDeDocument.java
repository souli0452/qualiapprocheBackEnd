package com.qualiapproche.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Un document à mettre en page — plan d'audit, rapport, fiche —, indépendant de son format.
 *
 * <p>Un seul modèle pour le PDF et le Word : chaque format n'est qu'un rendu de celui-ci. Deux
 * constructions séparées auraient fini par dire deux choses différentes du même dossier.</p>
 *
 * @param titre      « PLAN D'AUDIT », « RAPPORT D'AUDIT »
 * @param reference  la référence du document
 * @param sousTitre  ce dont il traite
 * @param sections   numérotées dans l'ordre
 * @param mentions   le pied de page : version, date d'édition, confidentialité ; nul s'il n'y en a pas
 */
public record ModeleDeDocument(String titre, String reference, String sousTitre, List<Section> sections, String mentions) {

    public ModeleDeDocument(String titre, String reference, String sousTitre, List<Section> sections) {
        this(titre, reference, sousTitre, sections, null);
    }

    /** Une section : des paragraphes, puis éventuellement un tableau. */
    public record Section(String titre, List<String> paragraphes, Tableau tableau) {

        public static Section texte(String titre, String... paragraphes) {
            List<String> retenus = new ArrayList<>();
            for (String p : paragraphes) {
                if (p != null && !p.isBlank()) {
                    retenus.add(p.strip());
                }
            }
            if (retenus.isEmpty()) {
                retenus.add("—");
            }
            return new Section(titre, retenus, null);
        }

        public static Section tableau(String titre, List<String> entetes, List<List<String>> lignes, String siVide) {
            if (lignes.isEmpty()) {
                return new Section(titre, List.of(siVide), null);
            }
            return new Section(titre, List.of(), new Tableau(entetes, lignes));
        }
    }

    public record Tableau(List<String> entetes, List<List<String>> lignes) {
    }
}
