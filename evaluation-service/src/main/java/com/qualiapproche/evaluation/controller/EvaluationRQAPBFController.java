package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.EvaluationRQAPBFDto;
import com.qualiapproche.evaluation.dto.NotationCritereDto;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.service.ReferentielRQAPBFService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import static com.qualiapproche.evaluation.controller.AuditUrls.EVALUATIONS_RQAPBF;

/** Les évaluations RQAPBF d'un audit : ouvertes, notées critère par critère, validées. */
@RestController
@RequiredArgsConstructor
@RequestMapping(EVALUATIONS_RQAPBF)
@RequirePermissions(
        create = {"audit-conduct"},
        update = {"audit-conduct"},
        read = {"audit-read", "audit-conduct", "audit-validate"},
        delete = {"audit-conduct"},
        validate = {"audit-validate", "audit-conduct"}
)
public class EvaluationRQAPBFController {

    private final ReferentielRQAPBFService service;

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EvaluationRQAPBFDto>>> deLAudit(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.evaluationsDeLAudit(auditId)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<EvaluationRQAPBFDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.lireEvaluation(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<EvaluationRQAPBFDto> create(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(service.ouvrirEvaluation(auditId));
    }

    /** Pose ou corrige les notes d'une série de critères ; le score se recalcule aussitôt. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/notations")
    public ResponseEntity<EvaluationRQAPBFDto> noter(@PathVariable("id") UUID id,
                                                     @Valid @RequestBody List<NotationCritereDto> notes) {
        return ResponseEntity.ok(service.noter(id, notes));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/notations/{noeudId}/publier")
    public ResponseEntity<EvaluationRQAPBFDto> publierCritere(@PathVariable("id") UUID id,
                                                              @PathVariable("noeudId") UUID noeudId) {
        return ResponseEntity.ok(service.publierCritere(id, noeudId));
    }

    /** Publie les critères en brouillon d'un niveau ; de tous les niveaux sans {@code niveau}. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/publier-groupe")
    public ResponseEntity<EvaluationRQAPBFDto> publierGroupe(
            @PathVariable("id") UUID id,
            @RequestParam(value = "niveau", required = false) NiveauNotationRQAPBF niveau) {
        return ResponseEntity.ok(service.publierGroupe(id, niveau));
    }

    /** Les écarts à transmettre au module Non-conformités : critères publiés au constat NC. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/ecarts")
    public ResponseEntity<ApiResponse<List<NotationCritereDto>>> ecarts(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.ecarts(id)));
    }

    /** Publie tout ce qui est noté et fige l'évaluation : les résultats officiels. */
    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/{id}/valider")
    public ResponseEntity<EvaluationRQAPBFDto> valider(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.validerEvaluation(id));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.supprimerEvaluation(id);
        return ResponseEntity.noContent().build();
    }
}
