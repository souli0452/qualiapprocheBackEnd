package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.client.ParametreClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lecture des réglages de l'organisation, pour le pied de page et la mise en copie.
 *
 * <p>Deux exigences seulement, mais qui portent tout le reste : le référentiel injoignable ne doit
 * jamais empêcher un envoi, et une étape à dix destinataires ne doit pas l'interroger dix fois pour
 * une donnée qui change deux fois par an.</p>
 */
class ReglagesOrganisationTest {

    private ReglagesOrganisation reglages(ParametreClient client) {
        ReglagesOrganisation reglages = new ReglagesOrganisation(client);
        ReflectionTestUtils.setField(reglages, "retentionSecondes", 600L);
        return reglages;
    }

    private Map<String, Object> reponse(Map<String, String> valeurs) {
        Map<String, Object> enveloppe = new LinkedHashMap<>();
        enveloppe.put("data", valeurs);
        return enveloppe;
    }

    @Test
    @DisplayName("Les valeurs renseignées sont rendues, indexées par clé")
    void valeursRenseignees_rendues() {
        ParametreClient client = mock(ParametreClient.class);
        when(client.valeursPubliques()).thenReturn(reponse(Map.of(
                "ORGANISATION_NOM", "QualiSira",
                "RESPONSABLE_QUALITE_EMAIL", " rq@qualisira.com ")));

        ReglagesOrganisation reglages = reglages(client);

        assertThat(reglages.valeur("ORGANISATION_NOM")).isEqualTo("QualiSira");
        // Une adresse copiée-collée avec des espaces ferait rejeter l'en-tête par le relais.
        assertThat(reglages.valeur("RESPONSABLE_QUALITE_EMAIL")).isEqualTo("rq@qualisira.com");
    }

    @Test
    @DisplayName("Un réglage absent ou vide rend null, et non une chaîne vide")
    void reglageAbsentOuVide_null() {
        ParametreClient client = mock(ParametreClient.class);
        when(client.valeursPubliques()).thenReturn(reponse(Map.of("CONTACT_TELEPHONE", "   ")));

        ReglagesOrganisation reglages = reglages(client);

        // Sans cela, chaque lecteur devrait refaire ce tri, et un en-tête Cc vide ferait rejeter le
        // message par certains relais.
        assertThat(reglages.valeur("CONTACT_TELEPHONE")).isNull();
        assertThat(reglages.valeur("ADRESSE_POSTALE")).isNull();
    }

    @Test
    @DisplayName("Référentiel injoignable : une carte vide, et non une exception")
    void referentielInjoignable_carteVide() {
        ParametreClient client = mock(ParametreClient.class);
        when(client.valeursPubliques()).thenThrow(new IllegalStateException("connection refused"));

        // Mieux vaut un message sans signature qu'un responsable jamais prévenu.
        assertThat(reglages(client).valeurs()).isEmpty();
    }

    @Test
    @DisplayName("Les réglages ne sont demandés qu'une fois : dix destinataires ne font pas dix appels")
    void reglages_misEnCache() {
        ParametreClient client = mock(ParametreClient.class);
        when(client.valeursPubliques()).thenReturn(reponse(Map.of("ORGANISATION_NOM", "QualiSira")));
        ReglagesOrganisation reglages = reglages(client);

        reglages.valeurs();
        reglages.valeur("ORGANISATION_NOM");
        reglages.valeur("CONTACT_EMAIL");

        verify(client, times(1)).valeursPubliques();
    }
}
