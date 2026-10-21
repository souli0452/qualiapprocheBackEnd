package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.NoeudReferentielDto;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.REFERENTIEL_RQAPBF;

/** L'arbre du référentiel RQAPBF : domaines, sous-domaines, critères. */
@RestController
@RequiredArgsConstructor
@RequestMapping(REFERENTIEL_RQAPBF)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-validate", "audit-config"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class ReferentielRQAPBFController {

    private final ReferentielRQAPBFService service;

    /** L'arbre entier, à partir de ses domaines. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<NoeudReferentielDto>>> arbre() {
        return ResponseEntity.ok(ApiResponse.success(service.arbre()));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<NoeudReferentielDto> create(@Valid @RequestBody NoeudReferentielDto dto) {
        return ResponseEntity.ok(service.creerNoeud(dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<NoeudReferentielDto> update(@PathVariable("id") UUID id,
                                                      @Valid @RequestBody NoeudReferentielDto dto) {
        return ResponseEntity.ok(service.modifierNoeud(id, dto));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.supprimerNoeud(id);
        return ResponseEntity.noContent().build();
    }
}
