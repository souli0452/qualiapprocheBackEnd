package com.qualiapproche.support.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.service.QmsDocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_TYPE;


@RestController
@RequestMapping(DOCUMENT_TYPE)
@RequiredArgsConstructor
@RequirePermissions(
        create = {"document-type-write"},
        update = {"document-type-write"},
        read = {"document-type-read", "document-type-write"},
        delete = {"document-type-write"}
)
public class QmsDocumentTypeController {

    private final QmsDocumentTypeService typeService;

    @GetMapping
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<QmsDocumentType>> getAllTypes() {
        return ResponseEntity.ok(typeService.getAllTypes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<QmsDocumentType> getTypeById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(typeService.getTypeById(id));
    }

    @PostMapping
    @PreAuthorize("@perm.canCreate(this)")
    public ResponseEntity<QmsDocumentType> createType(@RequestBody QmsDocumentType type) {
        return ResponseEntity.ok(typeService.createType(type));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<QmsDocumentType> updateType(@PathVariable("id") UUID id, @RequestBody QmsDocumentType type) {
        return ResponseEntity.ok(typeService.updateType(id, type));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.canDelete(this)")
    public ResponseEntity<Void> deleteType(@PathVariable("id") UUID id) {
        typeService.deleteType(id);
        return ResponseEntity.noContent().build();
    }
}
