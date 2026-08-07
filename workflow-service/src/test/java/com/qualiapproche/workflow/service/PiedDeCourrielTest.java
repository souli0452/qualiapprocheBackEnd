package com.qualiapproche.workflow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pied de page des courriels, composé des réglages de l'organisation.
 *
 * <p>Ce qui compte ici est ce qu'il fait <b>quand les réglages manquent</b> : un courriel d'étape
 * doit partir de toute façon. Des réglages vides ou indisponibles ne peuvent pas empêcher un
 * responsable d'être prévenu — le message part alors sans signature. La mise en cache et la
 * tolérance au référentiel injoignable sont vérifiées sur {@link ReglagesOrganisation}, qui en a la
 * charge.</p>
 */
class PiedDeCourrielTest {

    private PiedDeCourriel pied(Map<String, String> valeurs) {
        ReglagesOrganisation reglages = mock(ReglagesOrganisation.class);
        when(reglages.valeurs()).thenReturn(valeurs);
        return new PiedDeCourriel(reglages);
    }

    @Test
    @DisplayName("Le pied reprend les réglages renseignés")
    void reglagesRenseignes_composentLePied() {
        String corps = pied(Map.of(
                "ORGANISATION_NOM", "QualiSira",
                "CONTACT_EMAIL", "qualite@qualisira.com",
                "CONTACT_TELEPHONE", "+226 25 00 00 00"))
                .ajouterAu("<p>Une décision vous attend.</p>");

        assertThat(corps).contains("<p>Une décision vous attend.</p>");
        assertThat(corps).contains("QualiSira");
        assertThat(corps).contains("mailto:qualite@qualisira.com");
        assertThat(corps).contains("+226 25 00 00 00");
    }

    @Test
    @DisplayName("Aucun réglage renseigné : le corps part inchangé, sans cadre vide")
    void aucunReglage_corpsInchange() {
        assertThat(pied(Map.of()).ajouterAu("<p>Corps</p>")).isEqualTo("<p>Corps</p>");
    }

    @Test
    @DisplayName("Une valeur saisie ne peut pas injecter de balisage dans le message")
    void valeurs_echappees() {
        String corps = pied(Map.of("ORGANISATION_NOM", "<script>alert('x')</script>"))
                .ajouterAu("<p>Corps</p>");

        assertThat(corps).doesNotContain("<script>");
        assertThat(corps).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("Un site saisi sans protocole devient un lien absolu")
    void siteSansProtocole_lienAbsolu() {
        // Sans protocole, le lien serait relatif au client de messagerie, donc mort.
        assertThat(pied(Map.of("SITE_WEB", "qualisira.com")).ajouterAu("<p>Corps</p>"))
                .contains("https://qualisira.com");
    }

    @Test
    @DisplayName("Le logo est borné en hauteur : un fichier en pleine résolution noierait le message")
    void logo_borneEnHauteur() {
        String corps = pied(Map.of("LOGO_URL", "https://qualisira.com/logo.png"))
                .ajouterAu("<p>Corps</p>");

        assertThat(corps).contains("https://qualisira.com/logo.png");
        assertThat(corps).contains("max-height:48px");
    }
}
