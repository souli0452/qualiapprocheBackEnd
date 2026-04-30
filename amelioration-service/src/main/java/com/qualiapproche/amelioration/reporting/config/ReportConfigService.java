package com.qualiapproche.amelioration.reporting.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qualiapproche.amelioration.reporting.dto.ReportFormat;
import com.qualiapproche.amelioration.reporting.dto.ReportingInputDto;
import com.qualiapproche.amelioration.reporting.dto.ReportingResponseDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;


/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/05/22 à 08:56
 */
@Service
@AllArgsConstructor
@Slf4j
@SuppressWarnings("ALL")
public class ReportConfigService {
    private final ReportingTemplateConfig.ReportingTemplate reportingTemplate;

    /**
     * Building du rapport.
     *
     * @param inputDto
     * @param dto
     * @param parameterMap
     * @return ReportingResponseDto
     * @throws IOException
     * @throws JRException
     */
    public ReportingResponseDto buildReportNf(
            final ReportingInputDto inputDto, final Object dto,
            final HashMap<String, ? super Object> parameterMap) throws IOException, JRException {
        // recuperation du fichier jasper
        final String reportTemplate = reportingTemplate.getTemplateMap()
                .get(inputDto.getReportType().name());

        if (null == reportTemplate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "reportType " + inputDto.getReportType().name()
                            + " jasper template is missing in the config object.");
        }
        ReportConfigService.log.debug("reportTemplate name: {}", reportTemplate);
        InputStream fileInputStream = getClass().getResourceAsStream(reportTemplate);
        // convert DTO into the JsonDatasource
        InputStream jsonFile = this.convertDtoToInputStream(dto);
        JRDataSource jsonDataSource = new JsonDataSource(jsonFile);

        byte[] reportFile = this.genererRapport(fileInputStream, parameterMap, jsonDataSource, inputDto.getReportFormat());

        return new ReportingResponseDto(reportFile);
    }



    public ReportingResponseDto buildReport(
            final ReportingInputDto inputDto,
            final HashMap<String, ? super Object> parameterMap) throws IOException, JRException {
        // recuperation du fichier jasper
        final String reportTemplate = reportingTemplate.getTemplateMap()
                .get(inputDto.getReportType().name());

        if (null == reportTemplate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "reportType " + inputDto.getReportType().name()
                            + " jasper template is missing in the config object.");
        }
        ReportConfigService.log.debug("reportTemplate name: {}", reportTemplate);



        InputStream fileInputStream = getClass().getResourceAsStream(reportTemplate);
        byte[] reportFile = this.genererRapport(fileInputStream, parameterMap, new JREmptyDataSource(), inputDto.getReportFormat());


        return new ReportingResponseDto(reportFile);
    }


    /**
     * Convertir en inputStream.
     *
     * @param dto
     * @return InputStream
     * @throws IOException
     */
    private InputStream convertDtoToInputStream(final Object dto) throws IOException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        // Java object to JSON string
        String jsonString = mapper.writeValueAsString(dto);
        ReportConfigService.log.debug("\n Json String: {} \n", jsonString);
        InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
        return inputStream;
    }

    /**
     * generation du rapport en byte[].
     *
     * @param inputStream
     * @param parametres
     * @param jsonDataSource
     * @param format
     * @param source
     * @return byte[]
     */
    private byte[] genererRapport(
            @NotNull final InputStream inputStream,
            final HashMap<String, ? super Object> parametres,
            final JRDataSource jsonDataSource,
            @NotNull final ReportFormat format) {
        byte[] fluxFichier = null;
        JasperPrint jasperPrint = null;
        try {
            jasperPrint = JasperFillManager.fillReport(inputStream, parametres, jsonDataSource);

            switch (format) {
                case PDF:
                    fluxFichier = exportToPDF(jasperPrint);
                    break;
                case EXCEL:
                    fluxFichier = exportToExcel(jasperPrint);
                    break;
                case CSV:
                    fluxFichier = exportToCSV(jasperPrint);
                    break;
                case WORD:
                    fluxFichier = exportToWord(jasperPrint);
                    break;
                default:
                    return null;
            }
        } catch (Exception ex) {
            ReportConfigService.log.error("Erreur de generation du report", ex);
        }
        return fluxFichier;
    }

    /**
     * Cette méthode permet de generer un état sous format PDF.
     *
     * @param documentImprimable
     * @return un flux de bytes de données
     * @throws JRException
     */
    private byte[] exportToPDF(final JasperPrint documentImprimable) throws JRException {
        return JasperExportManager.exportReportToPdf(documentImprimable);
    }

    /**
     * Cette méthode permet de générer un état sous format Excel.
     *
     * @param doc
     * @return un tableau de bytes
     * @throws JRException
     */
    private byte[] exportToExcel(final JasperPrint doc) throws JRException {
        ByteArrayOutputStream excelReportStream = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(doc));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(excelReportStream));
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(false);
        configuration.setDetectCellType(true);
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        return excelReportStream.toByteArray();
    }

    /**
     * Cette méthode permet de générer un état sous format CSV.
     *
     * @param doc
     * @return un tableau de bytes
     * @throws JRException
     */
    private byte[] exportToCSV(final JasperPrint doc) throws JRException {
        ByteArrayOutputStream excelReportStream = new ByteArrayOutputStream();
        JRCsvExporter exporter = new JRCsvExporter();
        exporter.setExporterInput(new SimpleExporterInput(doc));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(excelReportStream));
        exporter.exportReport();
        return excelReportStream.toByteArray();

    }

    private byte[] exportToWord(final JasperPrint doc) throws JRException {
        ByteArrayOutputStream wordReportStream = new ByteArrayOutputStream();
        JRDocxExporter exporter = new JRDocxExporter();
        exporter.setExporterInput(new SimpleExporterInput(doc));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(wordReportStream));
        SimpleDocxExporterConfiguration configuration = new SimpleDocxExporterConfiguration();
        configuration.setEmbedFonts(Boolean.TRUE);
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        return wordReportStream.toByteArray();
    }
}
