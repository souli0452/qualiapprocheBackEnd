package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.dto.PlanAuditDto;
import com.qualiapproche.evaluation.service.PlanAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.AUDITS;

/** Le plan d'un audit : un par audit, sous le chemin de celui-ci. */
@RestController
@RequiredArgsConstructor
@RequestMapping(AUDITS + "/{auditId}/plan")
@RequirePermissions(
        update = {"audit-write"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-validate"},
        validate = {"audit-validate", "audit-write"}
)
public class PlanAuditController {

    private final PlanAuditService service;

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<PlanAuditDto> lire(@PathVariable("auditId") UUID auditId) {
        return ResponseEntity.ok(service.lire(auditId));
    }

    /** Pose le plan s'il n'existe pas, le met à jour sinon. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping
    public ResponseEntity<PlanAuditDto> enregistrer(@PathVariable("auditId") UUID auditId,
                                                    @Valid @RequestBody PlanAuditDto dto) {
        return ResponseEntity.ok(service.enregistrer(auditId, dto));
    }

    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/valider")
    public ResponseEntity<PlanAuditDto> valider(@PathVariable("auditId") UUID auditId) {
        return ResponseEntity.ok(service.valider(auditId));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/partager")
    public ResponseEntity<PlanAuditDto> partager(@PathVariable("auditId") UUID auditId) {
        return ResponseEntity.ok(service.partagerAvecAudite(auditId));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PostMapping(value = "/fichier", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlanAuditDto> charger(@PathVariable("auditId") UUID auditId,
                                                @RequestPart("fichier") MultipartFile fichier) {
        return ResponseEntity.ok(service.charger(auditId, fichier));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/fichier")
    public ResponseEntity<InputStreamResource> telecharger(@PathVariable("auditId") UUID auditId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plan-audit\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(service.contenuDuFichier(auditId)));
    }
}
