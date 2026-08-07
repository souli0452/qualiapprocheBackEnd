package com.qualiapproche.amelioration.utils;

import com.qualiapproche.amelioration.client.ReferentielClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lecture des réglages de l'organisation par le module des non-conformités.
 *
 * <p>Deux usages : l'adresse du responsable qualité, mis en copie de tout courriel de non-conformité,
 * et le seuil de relance des plans d'action. Aucun des deux ne peut faire échouer un envoi ni la
 * tournée de nuit — d'où le repli systématique.</p>
 */
class ReglagesOrganisationTest {

    private ReglagesOrganisation reglages(ReferentielClient client) {
        ReglagesOrganisation reglages = new ReglagesOrganisation(client);
        ReflectionTestUtils.setField(reglages, "retentionSecondes", 600L);
        return reglages;
    }

    @Test
    @DisplayName("Une valeur renseignée est rendue, débarrassée de ses espaces")
    void valeurRenseignee_rendue() {
        ReferentielClient client = mock(ReferentielClient.class);
        when(client.parametresPublics())
                .thenReturn(Map.of("RESPONSABLE_QUALITE_EMAIL", " rq@exemple.fr "));

        // Une adresse copiée-collée avec des espaces ferait rejeter l'en-tête par le relais.
        assertThat(reglages(client).valeur("RESPONSABLE_QUALITE_EMAIL")).isEqualTo("rq@exemple.fr");
    }

    @Test
    @DisplayName("Un réglage vide ou absent rend null, et non une chaîne vide")
    void reglageVide_null() {
        ReferentielClient client = mock(ReferentielClient.class);
        when(client.parametresPublics()).thenReturn(Map.of("RESPONSABLE_QUALITE_EMAIL", "  "));

        ReglagesOrganisation reglages = reglages(client);

        assertThat(reglages.valeur("RESPONSABLE_QUALITE_EMAIL")).isNull();
        assertThat(reglages.valeur("RESPONSABLE_QUALITE_NOM")).isNull();
    }

    @Test
    @DisplayName("Référentiel injoignable : le défaut s'applique, la tournée de rappels continue")
    void referentielInjoignable_defaut() {
        ReferentielClient client = mock(ReferentielClient.class);
        when(client.parametresPublics()).thenThrow(new IllegalStateException("connection refused"));

        // Mieux vaut relancer au délai habituel que ne relancer personne.
        assertThat(reglages(client).entier("RAPPEL_ECHEANCE_JOURS", 2)).isEqualTo(2);
        assertThat(reglages(client).valeur("RESPONSABLE_QUALITE_EMAIL")).isNull();
    }

    @Test
    @DisplayName("Un seuil renseigné est lu comme un nombre")
    void seuilRenseigne_lu() {
        ReferentielClient client = mock(ReferentielClient.class);
        when(client.parametresPublics()).thenReturn(Map.of("RAPPEL_ECHEANCE_JOURS", "5"));

        assertThat(reglages(client).entier("RAPPEL_ECHEANCE_JOURS", 2)).isEqualTo(5);
    }

    @Test
    @DisplayName("Un seuil illisible retombe sur le défaut plutôt que d'interrompre la tournée")
    void seuilIllisible_defaut() {
        ReferentielClient client = mock(ReferentielClient.class);
        when(client.parametresPublics()).thenReturn(Map.of("RAPPEL_ECHEANCE_JOURS", "cinq jours"));

        assertThat(reglages(client).entier("RAPPEL_ECHEANCE_JOURS", 2)).isEqualTo(2);
    }

    @Test
    @DisplayName("Les réglages ne sont demandés qu'une fois : une tournée de cent plans ne fait pas cent appels")
    void reglages_misEnCache() {
        ReferentielClient client = mock(ReferentielClient.class);
        when(client.parametresPublics()).thenReturn(Map.of("RAPPEL_ECHEANCE_JOURS", "5"));
        ReglagesOrganisation reglages = reglages(client);

        reglages.entier("RAPPEL_ECHEANCE_JOURS", 2);
        reglages.valeur("RESPONSABLE_QUALITE_EMAIL");
        reglages.entier("RAPPEL_ECHEANCE_JOURS", 2);

        verify(client, times(1)).parametresPublics();
    }
}
