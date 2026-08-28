package com.qualiapproche.workflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La liste des signataires d'une étape, telle qu'elle voyage entre l'écran, la colonne et le
 * contrôle d'habilitation.
 *
 * <p>Deux exigences opposées s'y rencontrent : l'éditeur doit retrouver <b>exactement</b> les
 * identifiants qu'il a envoyés, pour les rapprocher des personnes qu'il affiche ; la comparaison,
 * elle, ne peut pas se laisser défaire par une majuscule. D'où une écriture qui conserve, et une
 * comparaison qui normalise.</p>
 */
class CosignatairesTest {

    private static final String ANNE = "3f1b5c20-0000-4000-8000-00000000000a";
    private static final String BRUNO = "3f1b5c20-0000-4000-8000-00000000000b";

    @Test
    @DisplayName("Une étape qui ne nomme personne n'a pas de signataires")
    void listeVide() {
        assertThat(Cosignataires.lire(null)).isEmpty();
        assertThat(Cosignataires.lire("   ")).isEmpty();
        assertThat(Cosignataires.ecrire(null)).isNull();
        assertThat(Cosignataires.ecrire(List.of())).isNull();
        // Une liste qui ne contient que du vide n'est pas une liste : la colonne doit dire « rien ».
        assertThat(Cosignataires.ecrire(List.of("  ", ""))).isNull();
    }

    @Test
    @DisplayName("Les identifiants sont relus sans espaces superflus, dans l'ordre d'écriture")
    void lectureNormalisee() {
        assertThat(Cosignataires.lire("  " + ANNE + " , " + BRUNO + " ,, "))
                .containsExactly(ANNE, BRUNO);
    }

    @Test
    @DisplayName("L'écriture conserve la casse : l'éditeur doit retrouver ce qu'il a envoyé")
    void ecritureConserveLaCasse() {
        String majuscules = ANNE.toUpperCase(java.util.Locale.ROOT);

        assertThat(Cosignataires.ecrire(List.of(majuscules, BRUNO)))
                .isEqualTo(majuscules + "," + BRUNO);
    }

    @Test
    @DisplayName("La comparaison, elle, ignore la casse : une majuscule ne décide pas qui signe")
    void comparaisonInsensibleALaCasse() {
        Set<String> signataires = Cosignataires.lire(ANNE.toUpperCase(java.util.Locale.ROOT));

        assertThat(Cosignataires.designe(signataires, ANNE)).isTrue();
        assertThat(Cosignataires.designe(signataires, BRUNO)).isFalse();
    }

    @Test
    @DisplayName("L'auteur n'est écarté que s'il est à la fois l'appelant et un signataire de l'étape")
    void ecarteLAuteur() {
        Set<String> signataires = Cosignataires.lire(ANNE + "," + BRUNO);

        // Anne a déposé le dossier et signe ici : elle ne décide pas sur le sien.
        assertThat(Cosignataires.ecarteLAuteur(signataires, ANNE, ANNE)).isTrue();
        // Bruno signe aussi, mais ce n'est pas lui qui a déposé : il décide.
        assertThat(Cosignataires.ecarteLAuteur(signataires, ANNE, BRUNO)).isFalse();
        // L'auteur n'est pas de ceux qui signent ici : il n'y avait rien à séparer.
        assertThat(Cosignataires.ecarteLAuteur(Set.of(BRUNO), ANNE, ANNE)).isFalse();
        // Dossier sans auteur inscrit, ou appelant non identifié : la règle reste sans effet.
        assertThat(Cosignataires.ecarteLAuteur(signataires, null, ANNE)).isFalse();
        assertThat(Cosignataires.ecarteLAuteur(signataires, ANNE, null)).isFalse();
        assertThat(Cosignataires.ecarteLAuteur(Set.of(), ANNE, ANNE)).isFalse();
    }
}
