package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.ChecklistAuditDto;
import com.qualiapproche.evaluation.service.ChecklistAuditService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.CHECKLISTS;

/** Les checklists d'audit : rédigées en brouillon, publiées pour servir, archivées, dupliquées. */
@RestController
@RequiredArgsConstructor
@RequestMapping(CHECKLISTS)
@RequirePermissions(
        create = {"audit-write", "audit-config"},
        update = {"audit-write", "audit-config"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-validate", "audit-config"},
        delete = {"audit-write", "audit-config"}
)
public class ChecklistAuditController {

    private final ChecklistAuditService service;

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ChecklistAuditDto>>> all() {
        return ResponseEntity.ok(ApiResponse.success(service.lister()));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<ChecklistAuditDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<ChecklistAuditDto> create(@Valid @RequestBody ChecklistAuditDto dto) {
        return ResponseEntity.ok(service.creer(dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<ChecklistAuditDto> update(@PathVariable("id") UUID id, @Valid @RequestBody ChecklistAuditDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/publier")
    public ResponseEntity<ChecklistAuditDto> publier(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.publier(id));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/archiver")
    public ResponseEntity<ChecklistAuditDto> archiver(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.archiver(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/{id}/dupliquer")
    public ResponseEntity<ChecklistAuditDto> dupliquer(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.dupliquer(id));
    }
}
