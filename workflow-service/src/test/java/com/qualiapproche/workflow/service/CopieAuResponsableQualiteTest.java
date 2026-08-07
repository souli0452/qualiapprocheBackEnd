package com.qualiapproche.workflow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le responsable qualité en copie de ce qui sort d'une non-conformité.
 *
 * <p>Règle portée par le code et non par la configuration des circuits : l'y inscrire étape par
 * étape se serait perdu au premier circuit créé, et une non-conformité aurait suivi tout son
 * traitement sans que le responsable qualité en voie rien.</p>
 */
class CopieAuResponsableQualiteTest {

    private CopieAuResponsableQualite regle(String adresseDuRq) {
        ReglagesOrganisation reglages = mock(ReglagesOrganisation.class);
        when(reglages.valeur("RESPONSABLE_QUALITE_EMAIL")).thenReturn(adresseDuRq);
        return new CopieAuResponsableQualite(reglages);
    }

    @Test
    @DisplayName("Une non-conformité lui est copiée")
    void nonConformite_copiee() {
        assertThat(regle("rq@exemple.fr").pour("NON_CONFORMITE")).isEqualTo("rq@exemple.fr");
    }

    @Test
    @DisplayName("Un plan d'action aussi : il naît d'une non-conformité et en poursuit le traitement")
    void planAction_copie() {
        assertThat(regle("rq@exemple.fr").pour("PLAN_ACTION")).isEqualTo("rq@exemple.fr");
    }

    @Test
    @DisplayName("Le type est reconnu quelle que soit la casse : il circule en chaîne de caractères")
    void casseIndifferente() {
        assertThat(regle("rq@exemple.fr").pour(" non_conformite ")).isEqualTo("rq@exemple.fr");
    }

    @Test
    @DisplayName("Un document ou une demande ne lui est pas copié")
    void horsNonConformite_pasDeCopie() {
        // Copier chaque document validé noierait sa boîte, et la règle énoncée porte sur les
        // non-conformités.
        assertThat(regle("rq@exemple.fr").pour("DOCUMENT")).isNull();
        assertThat(regle("rq@exemple.fr").pour("DEMANDE_DOCUMENT")).isNull();
        assertThat(regle("rq@exemple.fr").pour(null)).isNull();
    }

    @Test
    @DisplayName("Adresse non renseignée : aucune copie, et l'envoi n'est pas empêché")
    void adresseNonRenseignee_aucuneCopie() {
        assertThat(regle(null).pour("NON_CONFORMITE")).isNull();
    }
}
