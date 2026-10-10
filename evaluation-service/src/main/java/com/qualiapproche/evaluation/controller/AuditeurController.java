package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.AuditeurDto;
import com.qualiapproche.evaluation.dto.FiltreVivier;
import com.qualiapproche.evaluation.service.AuditeurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.AUDITEURS;

/** Le vivier d'auditeurs. Supprimer un auditeur le désactive : son historique reste. */
@RestController
@RequiredArgsConstructor
@RequestMapping(AUDITEURS)
@RequirePermissions(
        create = {"audit-write", "audit-config"},
        update = {"audit-write", "audit-config"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-validate", "audit-config"},
        delete = {"audit-write", "audit-config"}
)
public class AuditeurController {

    private final AuditeurService service;

    /**
     * L'écran du vivier, page par page, avec ses filtres : {@code utilisateurIds} borne aux
     * personnes cherchées ; statut, structure, certification et tranche de moyenne d'évaluation.
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<AuditeurDto>> page(@ParameterObject FiltreVivier filtre,
                                                  @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.page(filtre, pageable));
    }

    /** Le vivier entier, pour les listes de choix. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<AuditeurDto>>> all() {
        return ResponseEntity.ok(ApiResponse.success(service.lister()));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<AuditeurDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<AuditeurDto> create(@Valid @RequestBody AuditeurDto dto) {
        return ResponseEntity.ok(service.creer(dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<AuditeurDto> update(@PathVariable("id") UUID id, @Valid @RequestBody AuditeurDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    /** Libre sur la période ? {@code auditId} écarte l'audit qu'on est en train de composer. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/disponibilite")
    public ResponseEntity<ApiResponse<Boolean>> disponibilite(
            @PathVariable("id") UUID id,
            @RequestParam("debut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(value = "auditId", required = false) UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.estDisponible(id, debut, fin, auditId)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/conflit-interet")
    public ResponseEntity<ApiResponse<Boolean>> conflitInteret(@PathVariable("id") UUID id,
                                                               @RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.aConflitInteret(id, auditId)));
    }
}
