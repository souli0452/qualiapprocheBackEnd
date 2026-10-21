package com.qualiapproche.amelioration.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'en-tête {@code Content-Disposition} ne se compose plus par concaténation : le nom vient d'un
 * dépôt, donc d'un client, et pouvait casser l'en-tête ou en injecter d'autres.
 */
class EnTeteFichierTest {

    @Test
    @DisplayName("Un saut de ligne dans le nom n'injecte aucun en-tête")
    void sautDeLigne_neutralise() {
        String valeur = EnTeteFichier.attachement("rapport.pdf\r\nSet-Cookie: session=volee");

        assertThat(valeur).doesNotContain("\r").doesNotContain("\n").startsWith("attachment;");
    }

    @Test
    @DisplayName("Un guillemet dans le nom ne ferme pas la valeur avant l'heure")
    void guillemet_neutralise() {
        String valeur = EnTeteFichier.attachement("a\"; filename=\"autre.exe");

        assertThat(valeur).isEqualTo("attachment; filename=\"a_; filename=_autre.exe\"");
    }

    @Test
    @DisplayName("Un nom accentué est encodé selon la RFC 5987")
    void nomAccentue_encode() {
        assertThat(EnTeteFichier.attachement("Procédure d'achat.pdf"))
                .contains("filename*=UTF-8''Proc%C3%A9dure%20d%27achat.pdf");
    }

    @Test
    @DisplayName("Un nom ASCII reste lisible, et un nom absent prend une valeur par défaut")
    void nomAscii_etAbsent() {
        assertThat(EnTeteFichier.attachement("rapport-audit")).isEqualTo("attachment; filename=\"rapport-audit\"");
        assertThat(EnTeteFichier.attachement(null)).isEqualTo("attachment; filename=\"fichier\"");
    }
}
