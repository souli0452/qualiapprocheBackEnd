package com.qualiapproche.evaluation.service.document;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.qualiapproche.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.List;

/** Le rendu d'un {@link DocumentAudit} en PDF ou en Word (.docx). */
@Slf4j
@Component
@RequiredArgsConstructor
public class RenduDocument {

    /** Les deux formats que proposent les écrans « Télécharger en PDF » et « Télécharger en Word ». */
    public enum Format {
        PDF("application/pdf", "pdf"),
        WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

        private final String typeMime;
        private final String extension;

        Format(String typeMime, String extension) {
            this.typeMime = typeMime;
            this.extension = extension;
        }

        public String typeMime() {
            return typeMime;
        }

        public String extension() {
            return extension;
        }
    }

    private static final String GABARIT = "documentAudit";

    private final TemplateEngine moteur;

    public byte[] rendre(DocumentAudit document, Format format) {
        return format == Format.PDF ? pdf(document) : word(document);
    }

    private byte[] pdf(DocumentAudit document) {
        Context contexte = new Context();
        contexte.setVariable("doc", document);
        String html = moteur.process(GABARIT, contexte);
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PdfRendererBuilder constructeur = new PdfRendererBuilder();
            constructeur.useFastMode();
            constructeur.withHtmlContent(html, null);
            constructeur.toStream(sortie);
            constructeur.run();
            return sortie.toByteArray();
        } catch (Exception e) {
            log.error("Rendu PDF de « {} » impossible", document.titre(), e);
            throw new BusinessException("Le document n'a pas pu être mis en page.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] word(DocumentAudit document) {
        try (XWPFDocument docx = new XWPFDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            paragraphe(docx, document.titre(), 18, true, ParagraphAlignment.CENTER);
            paragraphe(docx, document.sousTitre(), 12, false, ParagraphAlignment.CENTER);
            paragraphe(docx, "Réf. " + document.reference(), 9, false, ParagraphAlignment.CENTER);
            int numero = 1;
            for (DocumentAudit.Section section : document.sections()) {
                paragraphe(docx, numero++ + ". " + section.titre(), 12, true, ParagraphAlignment.LEFT);
                section.paragraphes().forEach(p -> paragraphe(docx, p, 10, false, ParagraphAlignment.LEFT));
                if (section.tableau() != null) {
                    tableau(docx, section.tableau());
                }
            }
            docx.write(sortie);
            return sortie.toByteArray();
        } catch (Exception e) {
            log.error("Rendu Word de « {} » impossible", document.titre(), e);
            throw new BusinessException("Le document n'a pas pu être mis en page.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static void paragraphe(XWPFDocument docx, String texte, int taille, boolean gras, ParagraphAlignment alignement) {
        XWPFParagraph p = docx.createParagraph();
        p.setAlignment(alignement);
        String[] lignes = (texte == null ? "" : texte).split("\n");
        XWPFRun run = p.createRun();
        run.setFontSize(taille);
        run.setBold(gras);
        for (int i = 0; i < lignes.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lignes[i]);
        }
    }

    private static void tableau(XWPFDocument docx, DocumentAudit.Tableau tableau) {
        List<String> entetes = tableau.entetes();
        XWPFTable t = docx.createTable(tableau.lignes().size() + 1, entetes.size());
        t.setWidth("100%");
        remplir(t.getRow(0), entetes, true);
        for (int i = 0; i < tableau.lignes().size(); i++) {
            remplir(t.getRow(i + 1), tableau.lignes().get(i), false);
        }
    }

    private static void remplir(XWPFTableRow ligne, List<String> valeurs, boolean gras) {
        for (int i = 0; i < valeurs.size(); i++) {
            XWPFParagraph p = ligne.getCell(i).getParagraphs().get(0);
            XWPFRun run = p.createRun();
            run.setFontSize(9);
            run.setBold(gras);
            run.setText(valeurs.get(i) == null ? "—" : valeurs.get(i));
        }
    }
}
