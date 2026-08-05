package com.qualiapproche.amelioration.reporting.service;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.amelioration.reporting.config.ReportConfigService;
import com.qualiapproche.amelioration.reporting.dto.PlanActionDto;
import com.qualiapproche.amelioration.reporting.dto.ReportingInputDto;
import com.qualiapproche.amelioration.reporting.dto.ReportingResponseDto;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.common.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("ALL")
public class ReportingService {
    private final ReportConfigService reportConfigService;
    private final ReportConfigService configService;
    private  final NonConformiteRepository conformiteRepository;


    /**
     * Génération des etat reporte.
     *
     * @param inputDto
     * @return ReportingResponseDto
     * @throws JRException
     * @throws IOException
     */
    public ReportingResponseDto generateReport(final ReportingInputDto inputDto) throws JRException, IOException {
        log.debug("Generating the report type: {}", inputDto.getReportType());
        ReportingResponseDto reportingResponseDto = null;
        switch (inputDto.getReportType()) {
            case NON_CONFORMITE:
                reportingResponseDto = this.generateNonConformite(inputDto);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun type de report trouvé.");
        }
        return reportingResponseDto;
    }

    /**
     * Génération des etat reporte.
     *
     * @param inputDto
     * @return ReportingResponseDto
     * @throws JRException
     * @throws IOException
     */



    private ReportingResponseDto generateNonConformite(final ReportingInputDto inputDto)
            throws IOException, JRException {
        NonConformite nonConformite = conformiteRepository.findById(inputDto.getEntityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "demande introuvable."));
        List<PlanActionDto> planActionList = nonConformite.getPlanActions().stream().map(plan -> new PlanActionDto(plan)).toList();

        return this.configService.buildReportNf(inputDto, planActionList, buildNonConformiteParam(nonConformite));
    }


    public HashMap<String, ? super Object> buildNonConformiteParam(NonConformite demande) throws IOException {
        final HashMap<String, ? super Object> parameterMap = new HashMap<>();

        // Paramètres obligatoires
        parameterMap.put("REFERENCE", demande.getNumeroReference() != null ? demande.getNumeroReference() : "N/A");
        parameterMap.put("VERSION", demande.getVersion() != null ? demande.getVersion() : "1.0");
        parameterMap.put("DATE_SOUMISSION", formatLocalDateTime(demande.getCreatedAt()));
        parameterMap.put("NUMERO", demande.getNumeroReference() != null ? demande.getNumeroReference() : "");

        // Cases à cocher (doivent être "X" ou "")
        parameterMap.put("CORRECTIVE", demande.getActionPreventive() != null ? "X" : "");
        parameterMap.put("PREVENTIVE", demande.getActionPreventive() == null ? "X" : "");

        // Champs texte
        parameterMap.put("SERVICE_PRODUIT", demande.getOrigineService() != null ? demande.getOrigineService() : "");
        parameterMap.put("ORIGINE", demande.getTypeNonConformiteLibelle() != null ? demande.getTypeNonConformiteLibelle() : "");
        parameterMap.put("DESCRIPTION", demande.getJustification() != null ? demande.getJustification() : "");
        parameterMap.put("REACTION",  "Pas de réaction");
        parameterMap.put("JUSTIFICATION_PILOTE", demande.getJustificationPilote() != null ? stripTags(demande.getJustificationPilote()) : "");
        parameterMap.put("JUSTIFICATION_RS", demande.getJustificationRs() != null ? stripTags(demande.getJustificationRs()) : "");
        parameterMap.put("PARTICIPANTS",
                demande.getParticipants() != null && demande.getParticipants().getFullNames() != null
                        ? String.join(", ", demande.getParticipants().getFullNames())
                        : ""
        );

        // Dates supplémentaires
        parameterMap.put("DATE_PILOTE", formatLocalDateTime(demande.getDateVisaEmetteur()));
        parameterMap.put("DATE_RS", demande.getDateSuivi() != null ? formatLocalDateTime(demande.getDateSuivi()) : "");

        // Cases à cocher pour pertinence
        String pertinencePilote = demande.getPertinancePilote();
        parameterMap.put("PERTINENCE_OUI", "Oui".equalsIgnoreCase(pertinencePilote) ? "X" : "");
        parameterMap.put("PERTINENCE_NON", "Non".equalsIgnoreCase(pertinencePilote) ? "X" : "");

        String pertinenceRs = demande.getPertinanceRs();
        parameterMap.put("RS_PERTINENCE_OUI", "Oui".equalsIgnoreCase(pertinenceRs) ? "X" : "");
        parameterMap.put("RS_PERTINENCE_NON", "Non".equalsIgnoreCase(pertinenceRs) ? "X" : "");

        return parameterMap;
    }



    public static String stripTags(String html) {
        return html.replaceAll("<[^>]*>", "").trim();
    }
    public static String formatLocalDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateTime.format(formatter);
    }

    private String formatBigDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);

        return decimalFormat.format(value);
    }

    private String generateQrcodeUrl(UUID demandeId, TypeDemande typeDemande) {
        String type = CryptoUtils.encrypt(String.format("%s", typeDemande));

        return String.format("%s?key=%s&type=%s", "", demandeId, type);
    }

}



