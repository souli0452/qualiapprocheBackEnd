package com.qualiapproche.evaluation.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.qualiapproche.evaluation.service.NotificationsAuditService;
import com.qualiapproche.evaluation.dto.NotificationsAuditResumeDto;
import com.qualiapproche.common.dto.NotificationDto;
import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.AvancementDto;
import com.qualiapproche.evaluation.dto.ComparaisonDto;
import com.qualiapproche.evaluation.dto.NotationCritereDto;
import com.qualiapproche.evaluation.dto.TableauDeBordDto;
import com.qualiapproche.evaluation.service.PilotageAuditService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final NotificationsAuditService notifications;

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

    /** Les critères RQAP-BF publiés au constat NC, de toute la direction ou d'un audit, page par page. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/ecarts-rqapbf")
    public ResponseEntity<Page<NotationCritereDto>> ecartsRqapbf(
            @RequestParam(value = "auditId", required = false) UUID auditId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.ecartsRqapbf(auditId, pageable));
    }

    @Operation(summary = "Ce que l'utilisateur connecté a en attente dans le module Audit",
            description = "Lignes de la cloche, recalculées à chaque appel et jamais conservées")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> notifications() {
        return ResponseEntity.ok(ApiResponse.success(notifications.pourLAppelant()));
    }

    @Operation(summary = "Ce que l'utilisateur connecté a en attente, en nombres",
            description = "Un compteur par onglet du module, et leur somme pour la pastille du menu")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/notifications/resume")
    public ResponseEntity<NotificationsAuditResumeDto> resumeDesNotifications() {
        return ResponseEntity.ok(notifications.resume());
    }
}
