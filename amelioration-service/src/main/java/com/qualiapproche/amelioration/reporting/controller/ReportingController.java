package com.qualiapproche.amelioration.reporting.controller;


import com.qualiapproche.amelioration.reporting.dto.ReportingInputDto;
import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.amelioration.reporting.dto.ReportingResponseDto;
import com.qualiapproche.amelioration.reporting.service.ReportingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static com.qualiapproche.common.utils.ApiUrls.CREATE_REPORTING_ROOT_URL;


/**
 * Édition des états imprimés d'une non-conformité.
 *
 * <p>Le point d'entrée n'avait aucune garde : tout compte authentifié obtenait l'état de n'importe
 * quel dossier en citant son identifiant — constat, justifications du pilote et du responsable
 * qualité, participants. Il exige désormais une permission de lecture des non-conformités, la même
 * que la fiche à l'écran, et le service applique au dossier demandé la même règle de visibilité
 * qu'elle (voir {@code NonConformiteService#peutConsulter}).</p>
 *
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 08/03/2022 à 14:19:09
 */
@RestController
@Slf4j
@RequestMapping(CREATE_REPORTING_ROOT_URL)
@RequiredArgsConstructor
@RequirePermissions(
        // Celles de la lecture d'une non-conformité (NonConformiteController) : l'état imprimé
        // n'en dit pas plus que la fiche.
        read = {"nc-read", "nc-write", "NC_READ", "CONSULTATION_NC", "SUBMIT_NC", "TRAITEMENT_NC"}
)
public class ReportingController {
    private final ReportingService reportingService;


    @PreAuthorize("@perm.canRead(this)")
    @PostMapping()
    public ResponseEntity<byte[]> generateReport(@RequestBody @Valid final ReportingInputDto inputDto)
            throws IOException, JRException {
        log.debug("Generate report: {}", inputDto);

        ReportingResponseDto responseDto = reportingService.generateReport(inputDto);
        return new ResponseEntity<>(responseDto.getReportFile(), HttpStatus.CREATED);
    }
}
