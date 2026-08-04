package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.TypeNonConformiteDto;
import com.qualiapproche.amelioration.service.TypeNonConformiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
public class TypeNonConformiteController {

    private final TypeNonConformiteService typeNonConformiteService;

    @Operation(summary = "Créer un type de NC", description = "Définit une nouvelle catégorie de non-conformité")
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_TYPE_NON_CONFORMITE)
    public ResponseEntity<TypeNonConformiteDto> create(@RequestBody TypeNonConformiteDto typeNonConformiteDto) {
        TypeNonConformiteDto typeNonConformiteDto1 = typeNonConformiteService.create(typeNonConformiteDto);
        return new ResponseEntity<>(typeNonConformiteDto1, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_TYPE_NON_CONFORMITE)
    public ResponseEntity<TypeNonConformiteDto> update(@RequestBody TypeNonConformiteDto typeNonConformiteDto) {
        TypeNonConformiteDto TypeNonConformiteDto1 = typeNonConformiteService.update(typeNonConformiteDto);
        return new ResponseEntity<>(TypeNonConformiteDto1, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_TYPE_NON_CONFORMITE)
    public ResponseEntity<Page<TypeNonConformiteDto>> allTypeNonConformite(@ParameterObject Pageable pageable) {
        Page<TypeNonConformiteDto> typeNonConformiteDtos  = typeNonConformiteService.getAll(pageable);
        return new ResponseEntity<>(typeNonConformiteDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_TYPE_NON_CONFORMITE_BY_ID)
    public ResponseEntity<TypeNonConformiteDto> getById(@PathVariable UUID id) {
        TypeNonConformiteDto typeNonConformiteDto  = typeNonConformiteService.getById(id);
        return new ResponseEntity<>(typeNonConformiteDto, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_TYPE_NON_CONFORMITE)
    public void deleteyId(@PathVariable UUID id) {
        typeNonConformiteService.delete(id);
    }
}
