package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.AvancementDto;
import com.qualiapproche.evaluation.dto.ComparaisonDto;
import com.qualiapproche.evaluation.dto.NotationCritereDto;
import com.qualiapproche.evaluation.dto.TableauDeBordDto;
import com.qualiapproche.evaluation.service.PilotageAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.AUDITS;

/**
 * Les vues de pilotage : tableau de bord (A1), suivi (D16), avancement (C9), historique (D17),
 * écarts RQAP-BF (D18). Lecture seule.
 *
 * <p>« Mes audits » et la liste des audits en retard du suivi sont des filtres du programme :
 * {@code GET /audits?mesAudits=true}, {@code GET /audits?enRetard=true}.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(AUDITS)
@RequirePermissions(read = {"audit-read", "audit-write", "audit-conduct", "audit-validate"})
public class PilotageAuditController {

    private final PilotageAuditService service;

    /** Indicateurs de l'année ; l'année en cours par défaut. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/tableau-de-bord")
    public ResponseEntity<TableauDeBordDto> tableauDeBord(@RequestParam(value = "annee", required = false) Integer annee) {
        return ResponseEntity.ok(service.tableauDeBord(annee == null ? LocalDate.now().getYear() : annee));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/avancement")
    public ResponseEntity<AvancementDto> avancement(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.avancement(id));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/comparaison")
    public ResponseEntity<ComparaisonDto> comparer(
            @RequestParam("debut1") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut1,
            @RequestParam("fin1") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin1,
            @RequestParam("debut2") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut2,
            @RequestParam("fin2") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin2) {
        return ResponseEntity.ok(service.comparer(debut1, fin1, debut2, fin2));
    }

    /** Les critères RQAP-BF publiés au constat NC, de toute la direction ou d'un audit. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/ecarts-rqapbf")
    public ResponseEntity<ApiResponse<List<NotationCritereDto>>> ecartsRqapbf(
            @RequestParam(value = "auditId", required = false) UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.ecartsRqapbf(auditId)));
    }
}
