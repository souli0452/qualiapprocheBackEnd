package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.SourceNonConformiteDto;
import com.qualiapproche.amelioration.service.SourceNonConformiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(TYPE_NON_CONFORMITE_ROOT_URL)
@Tag(name = "Types de Non-Conformité", description = "Paramétrage des catégories de non-conformité")
@RequirePermissions(
        create = {"type-nc-write", "NC_ORIGIN_MANAGE"},
        update = {"type-nc-write", "NC_ORIGIN_MANAGE"},
        read = {"type-nc-read", "type-nc-write", "NC_READ", "NC_ORIGIN_MANAGE"},
        delete = {"type-nc-write", "NC_ORIGIN_MANAGE"}
)
public class SourceNonConformiteController {

    private final SourceNonConformiteService sourceNonConformiteService;

    @Operation(summary = "Créer un type de NC", description = "Définit une nouvelle catégorie de non-conformité")
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_TYPE_NON_CONFORMITE)
    public ResponseEntity<SourceNonConformiteDto> create(@RequestBody SourceNonConformiteDto sourceNonConformiteDto) {
        SourceNonConformiteDto sourceNonConformiteDto1 = sourceNonConformiteService.create(sourceNonConformiteDto);
        return new ResponseEntity<>(sourceNonConformiteDto1, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_TYPE_NON_CONFORMITE)
    public ResponseEntity<SourceNonConformiteDto> update(@RequestBody SourceNonConformiteDto sourceNonConformiteDto) {
        SourceNonConformiteDto sourceNonConformiteDto1 = sourceNonConformiteService.update(sourceNonConformiteDto);
        return new ResponseEntity<>(sourceNonConformiteDto1, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_TYPE_NON_CONFORMITE)
    public ResponseEntity<Page<SourceNonConformiteDto>> allSourceNonConformite(@ParameterObject Pageable pageable) {
        Page<SourceNonConformiteDto> sourceNonConformiteDtos = sourceNonConformiteService.getAll(pageable);
        return new ResponseEntity<>(sourceNonConformiteDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_TYPE_NON_CONFORMITE_BY_ID)
    public ResponseEntity<SourceNonConformiteDto> getById(@PathVariable UUID id) {
        SourceNonConformiteDto sourceNonConformiteDto = sourceNonConformiteService.getById(id);
        return new ResponseEntity<>(sourceNonConformiteDto, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_TYPE_NON_CONFORMITE)
    public void deleteyId(@PathVariable UUID id) {
        sourceNonConformiteService.delete(id);
    }
}
