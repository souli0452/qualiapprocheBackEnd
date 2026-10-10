package com.qualiapproche.amelioration.numerotation;

import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.SerieNumeroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Le numéro d'une NC garde son format ; seule sa série change de mécanisme. */
class NumerotationDesNonConformitesTest {

    private final NonConformiteRepository nonConformites = mock(NonConformiteRepository.class);
    private final NumerotationDesNonConformites numerotation =
            new NumerotationDesNonConformites(nonConformites, mock(SerieNumeroRepository.class));

    @Test
    @DisplayName("Format inchangé : NFQT-SIGLE-ANNÉE-00001, sigle en majuscules sans espace")
    void format_inchange() {
        var c = new NumerotationDesNonConformites.Contexte("s-1", "dqa  qualite", 2026);
        assertThat(numerotation.formater(c, 121)).isEqualTo("NFQT-DQA_QUALITE-2026-00121");
        assertThat(numerotation.cle(c)).isEqualTo("NC|s-1|2026");
    }

    @Test
    @DisplayName("Une série neuve repart du dernier numéro émis par la structure dans l'année")
    void reprend_l_existant() {
        when(nonConformites.findDernierNumero("s-1", 2026)).thenReturn(120);
        assertThat(numerotation.valeurInitiale(new NumerotationDesNonConformites.Contexte("s-1", "DQA", 2026))).isEqualTo(120);
        assertThat(numerotation.valeurInitiale(new NumerotationDesNonConformites.Contexte("s-2", "GSI", 2026))).isZero();
    }
}
