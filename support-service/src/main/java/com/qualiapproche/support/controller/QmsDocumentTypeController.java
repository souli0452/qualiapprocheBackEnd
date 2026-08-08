package com.qualiapproche.support.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.service.QmsDocumentTypeService;
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

import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_TYPE;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


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

    /**
     * Page des types documentaires, filtrée sur {@code search} le cas échéant.
     *
     * <p>Une {@code Page} explicite, là où une liste nue était paginée d'office par
     * {@code GlobalResponseHandler} — donc tronquée à dix types sans que rien ne le dise,
     * et sans moyen de chercher au-delà.</p>
     */
    @GetMapping
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<Page<QmsDocumentType>> getAllTypes(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(typeService.getTypes(search, pageable));
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
