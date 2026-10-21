package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.EvaluationAuditeurDto;
import com.qualiapproche.evaluation.service.EvaluationAuditeurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.EVALUATIONS_AUDITEUR;

/**
 * L'évaluation des auditeurs, critère par critère.
 *
 * <p>Réservée à {@code audit-validate} : noter un collègue n'est pas un geste d'équipier.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(EVALUATIONS_AUDITEUR)
@RequirePermissions(
        create = {"audit-validate"},
        update = {"audit-validate"},
        read = {"audit-validate", "audit-read"},
        validate = {"audit-validate"}
)
public class EvaluationAuditeurController {

    private final EvaluationAuditeurService service;

    /** Par audit, ou par auditeur : l'un des deux est requis. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EvaluationAuditeurDto>>> all(
            @RequestParam(value = "auditId", required = false) UUID auditId,
            @RequestParam(value = "auditeurId", required = false) UUID auditeurId) {
        if (auditId != null) {
            return ResponseEntity.ok(ApiResponse.success(service.deLAudit(auditId)));
        }
        if (auditeurId != null) {
            return ResponseEntity.ok(ApiResponse.success(service.deLAuditeur(auditeurId)));
        }
        throw new BusinessException("Précisez l'audit ou l'auditeur dont lire les évaluations.", HttpStatus.BAD_REQUEST);
    }

    /** Évalue un auditeur sur un critère ; une seconde saisie du même critère corrige la première. */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<EvaluationAuditeurDto> evaluer(@Valid @RequestBody EvaluationAuditeurDto dto) {
        return ResponseEntity.ok(service.evaluer(dto));
    }

    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/{id}/valider")
    public ResponseEntity<EvaluationAuditeurDto> valider(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.valider(id));
    }
}
