package com.qualiapproche.workflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les faits établis sur un dossier : ce que le module métier déclare, ce que le circuit exige.
 *
 * <p>C'est le seul point de contact entre une règle métier et une condition de circuit. Une lecture
 * qui laisserait passer un espace ou une casse ferait échouer la comparaison sans que rien ne le
 * signale : la transition resterait fermée, et l'on chercherait l'erreur du côté du circuit.</p>
 */
class FaitsDuDossierTest {

    @Test
    @DisplayName("Un dossier sans fait n'en porte aucun")
    void aucunFait() {
        assertThat(FaitsDuDossier.lire(null)).isEmpty();
        assertThat(FaitsDuDossier.lire("   ")).isEmpty();
    }

    @Test
    @DisplayName("La casse et les espaces ne font pas échouer la comparaison")
    void lectureTolerante() {
        assertThat(FaitsDuDossier.contient(" plans_action_soldes , autre ", "PLANS_ACTION_SOLDES")).isTrue();
        assertThat(FaitsDuDossier.contient("PLANS_ACTION_SOLDES", "plans_action_soldes")).isTrue();
    }

    @Test
    @DisplayName("Un fait absent n'est pas tenu pour établi")
    void faitAbsent() {
        assertThat(FaitsDuDossier.contient("AUTRE_FAIT", "PLANS_ACTION_SOLDES")).isFalse();
        assertThat(FaitsDuDossier.contient(null, "PLANS_ACTION_SOLDES")).isFalse();
    }

    @Test
    @DisplayName("Une transition sans condition est franchissable")
    void aucuneConditionExigee() {
        // C'est le cas de la plupart des transitions : n'exiger aucun fait ne doit rien bloquer.
        assertThat(FaitsDuDossier.contient(null, null)).isTrue();
        assertThat(FaitsDuDossier.contient(null, "  ")).isTrue();
    }

    @Test
    @DisplayName("Un dossier sans fait s'écrit nul, et non chaîne vide")
    void ecritureDUnEnsembleVide() {
        // La colonne doit dire « aucun fait », pas « un fait sans nom ».
        assertThat(FaitsDuDossier.ecrire(Set.of())).isNull();
        assertThat(FaitsDuDossier.ecrire(null)).isNull();
    }

    @Test
    @DisplayName("L'écriture normalise et se relit à l'identique")
    void allerRetour() {
        Set<String> faits = new LinkedHashSet<>(Set.of("plans_action_soldes", "EFFICACITE_MESUREE"));

        String stocke = FaitsDuDossier.ecrire(faits);

        assertThat(FaitsDuDossier.lire(stocke))
                .containsExactlyInAnyOrder("PLANS_ACTION_SOLDES", "EFFICACITE_MESUREE");
    }
}
