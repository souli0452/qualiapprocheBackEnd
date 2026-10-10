package com.qualiapproche.storage;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Un même document rendu dans les deux formats que proposent les écrans. */
class RenduDocumentTest {

    private final RenduDocument rendu = new RenduDocument();

    private final ModeleDeDocument document = new ModeleDeDocument("RAPPORT D'AUDIT", "QS-RAP-2026-001",
            "AUD-2026-012 — Gestion documentaire", List.of(
            ModeleDeDocument.Section.texte("Objectifs de l'audit", "Vérifier la conformité §7.5 & <traçabilité>."),
            ModeleDeDocument.Section.tableau("Synthèse quantitative des constats par nature",
                    List.of("Nature du constat", "Nombre"),
                    List.of(List.of("Non-conformité mineure", "1"), List.of("Point fort", "2")), "Aucun constat."),
            ModeleDeDocument.Section.tableau("Signatures", List.of("Signataire"), List.of(), "Pas encore envoyé.")));

    @Test
    @DisplayName("Le document rendu au format PDF est bien un PDF")
    void le_pdf_est_un_pdf() {
        byte[] pdf = rendu.rendre(document, RenduDocument.Format.PDF);
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Le document Word se relit et porte les sections numérotées et le tableau")
    void le_word_se_relit_et_porte_les_sections_numerotees_et_le_tableau() throws Exception {
        byte[] docx = rendu.rendre(document, RenduDocument.Format.WORD);
        try (XWPFDocument relu = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor texte = new XWPFWordExtractor(relu)) {
            assertThat(texte.getText()).contains("RAPPORT D'AUDIT", "1. Objectifs de l'audit", "<traçabilité>",
                    "Non-conformité mineure", "3. Signatures", "Pas encore envoyé.");
            assertThat(relu.getTables()).hasSize(1);
        }
    }
}
