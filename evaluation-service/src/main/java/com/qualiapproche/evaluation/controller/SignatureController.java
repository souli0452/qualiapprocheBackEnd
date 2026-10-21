package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.MotifDto;
import com.qualiapproche.evaluation.dto.SignatureDto;
import com.qualiapproche.evaluation.service.SignatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.AUDITS;

/**
 * Le circuit de signature du rapport.
 *
 * <p>Signer ou refuser ne demande que de lire l'audit : c'est la désignation dans le circuit qui
 * fait le droit, et le service vérifie que c'est bien le tour de l'appelant. Un directeur appelé à
 * signer n'a pas à détenir les permissions de conduite de l'audit.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(AUDITS + "/{auditId}/signatures")
@RequirePermissions(
        update = {"audit-write", "audit-validate"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-validate"}
)
public class SignatureController {

    private final SignatureService service;

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SignatureDto>>> circuit(@PathVariable("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.circuit(auditId)));
    }

    /** Pose le circuit, dans l'ordre de la liste ; efface les visas d'un circuit précédent. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping
    public ResponseEntity<ApiResponse<List<SignatureDto>>> poser(@PathVariable("auditId") UUID auditId,
                                                                 @Valid @RequestBody List<SignatureDto> signataires) {
        return ResponseEntity.ok(ApiResponse.success(service.poserLeCircuit(auditId, signataires)));
    }

    /** Remplace un signataire qui n'a pas encore signé ; les visas déjà donnés restent. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{signatureId}/remplacer")
    public ResponseEntity<ApiResponse<List<SignatureDto>>> remplacer(@PathVariable("auditId") UUID auditId,
                                                                     @PathVariable("signatureId") UUID signatureId,
                                                                     @RequestBody SignatureDto nouveau) {
        return ResponseEntity.ok(ApiResponse.success(service.remplacer(auditId, signatureId, nouveau)));
    }

    /** Relance par courriel un signataire dont le visa est attendu. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PostMapping("/{signatureId}/relancer")
    public ResponseEntity<Void> relancer(@PathVariable("auditId") UUID auditId,
                                         @PathVariable("signatureId") UUID signatureId) {
        service.relancer(auditId, signatureId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@perm.canRead(this)")
    @PutMapping("/signer")
    public ResponseEntity<SignatureDto> signer(@PathVariable("auditId") UUID auditId) {
        return ResponseEntity.ok(service.signer(auditId));
    }

    @PreAuthorize("@perm.canRead(this)")
    @PutMapping("/refuser")
    public ResponseEntity<SignatureDto> refuser(@PathVariable("auditId") UUID auditId, @RequestBody MotifDto motif) {
        return ResponseEntity.ok(service.refuser(auditId, motif.getMotif()));
    }
}
