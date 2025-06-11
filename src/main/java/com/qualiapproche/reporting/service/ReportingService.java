package com.qualiapproche.reporting.service;
import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.entities.NonConformite;
import com.qualiapproche.enumeration.TypeDemande;
import com.qualiapproche.reporting.config.ReportConfigService;
import com.qualiapproche.reporting.config.ReportConstant;
import com.qualiapproche.reporting.dto.JasperPlanActionDto;
import com.qualiapproche.reporting.dto.ReportingInputDto;
import com.qualiapproche.reporting.dto.ReportingResponseDto;
import com.qualiapproche.repository.NonConformiteRepository;
import com.qualiapproche.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import static java.util.Objects.nonNull;


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
        List<JasperPlanActionDto> planActionDtos =nonConformite.getPlanActions().stream().map(planAction -> new JasperPlanActionDto(planAction)).toList();
        return this.configService.buildReport(inputDto,planActionDtos,buildNonConformiteParam(nonConformite));
    }


    public HashMap<String, ? super Object> buildNonConformiteParam(NonConformite demande) throws IOException {

        final HashMap<String, ? super Object> parameterMap = new HashMap<>();
        parameterMap.put(ReportConstant.JASPER_PARAM_VERSION,
                demande.getVersion() != null ? demande.getVersion() : "1.0");
        parameterMap.put(ReportConstant.JASPER_PARAM_DESCRIPTION,
                demande.getJustification() != null ? demande.getJustification() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_DATE_SOUMISSION,
                demande.getCreatedAt() != null ? demande.getCreatedAt() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_JUSTIFICATION_PILOTE,
                demande.getJustificationPilote() != null ? demande.getJustificationPilote() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_JUSTIFICATION_RS,
                demande.getJustificationRs() != null ? demande.getJustificationRs() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_PARICIPANTS,
                demande.getParticipants() != null ? demande.getParticipants().getFullNames() : "");
            parameterMap.put(ReportConstant.JASPER_PARAM_PERTINENCE_OUI,
                    demande.getPertinancePilote().compareToIgnoreCase("Oui")==0? "X" : "");
            parameterMap.put(ReportConstant.JASPER_PARAM_PERTINENCE_NON,
                    demande.getPertinancePilote().compareToIgnoreCase("Non")==0? "X" : "");

            parameterMap.put(ReportConstant.JASPER_PARAM_RS_PERTINENCE_OUI,
                    demande.getPertinanceRs().compareToIgnoreCase("Oui")==0? "X" : "");
            parameterMap.put(ReportConstant.JASPER_PARAM_RS_PERTINENCE_NON,
                    demande.getPertinanceRs().compareToIgnoreCase("Non")==0? "X" : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_NUMERO,
                demande.getNumeroReference() != null ? demande.getNumeroReference() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_REACTION,
                demande.getJustification() != null ? demande.getJustification() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_SERVICE_PRODUIT,
                demande.getJustification() != null ? demande.getJustification() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_CORRECTIVE,
                demande.getJustification() != null ? demande.getJustification() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_PREVENTIVE,
                demande.getJustification() != null ? demande.getJustification() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_DATE_RS,
                demande.getDateClotureRq() != null ? demande.getDateClotureRq() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_DATE_PILOTE,
                demande.getDateVisaEmetteur() != null ? demande.getDateVisaEmetteur() : "");
        parameterMap.put(ReportConstant.JASPER_PARAM_REFERENCE,
                demande.getNumeroReference() != null ? demande.getNumeroReference() : "");
        return parameterMap;
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



