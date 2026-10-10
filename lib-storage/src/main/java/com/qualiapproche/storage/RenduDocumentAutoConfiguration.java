package com.qualiapproche.storage;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Câblage du rendu PDF et Word.
 *
 * <p>Ses bibliothèques — Thymeleaf, openhtmltopdf, POI — sont facultatives dans lib-storage : un
 * service qui ne fait que déposer des fichiers ne les embarque pas. Celui qui exporte des
 * documents les déclare, et le rendu apparaît. Indépendant de {@code minio.url} : mettre en page
 * ne demande pas de serveur de fichiers.</p>
 */
@AutoConfiguration
@ConditionalOnClass(name = {
        "org.thymeleaf.spring6.SpringTemplateEngine",
        "com.openhtmltopdf.pdfboxout.PdfRendererBuilder",
        "org.apache.poi.xwpf.usermodel.XWPFDocument"})
public class RenduDocumentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RenduDocument renduDocument() {
        return new RenduDocument();
    }
}
