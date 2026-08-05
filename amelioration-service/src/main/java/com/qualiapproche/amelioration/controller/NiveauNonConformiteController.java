package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.NiveauNonConformiteDto;
import com.qualiapproche.amelioration.service.NiveauNonConformiteService;
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
@RequestMapping(NIVEAU_NON_CONFORMITE_ROOT_URL)
@RequirePermissions(
        create = {"niveau-nc-write", "NC_LEVEL_MANAGE"},
        update = {"niveau-nc-write", "NC_LEVEL_MANAGE"},
        read = {"niveau-nc-read", "niveau-nc-write", "NC_READ", "NC_LEVEL_MANAGE"},
        delete = {"niveau-nc-write", "NC_LEVEL_MANAGE"}
)
public class NiveauNonConformiteController {

    private final NiveauNonConformiteService niveauNonConformiteService;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_NIVEAU_NON_CONFORMITE)
    public ResponseEntity<NiveauNonConformiteDto> create(@RequestBody NiveauNonConformiteDto niveauNonConformiteDto) {
        NiveauNonConformiteDto niveauNonConformiteDto1 = niveauNonConformiteService.create(niveauNonConformiteDto);
        return new ResponseEntity<>(niveauNonConformiteDto1, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_NIVEAU_NON_CONFORMITE)
    public ResponseEntity<NiveauNonConformiteDto> update(@RequestBody NiveauNonConformiteDto niveauNonConformiteDto) {
        NiveauNonConformiteDto niveauNonConformiteDto1 = niveauNonConformiteService.update(niveauNonConformiteDto);
        return new ResponseEntity<>(niveauNonConformiteDto1, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_NIVEAU_NON_CONFORMITE)
    public ResponseEntity<Page<NiveauNonConformiteDto>> allNiveauNonConformite(@ParameterObject Pageable pageable) {
        Page<NiveauNonConformiteDto> niveauNonConformiteDtos = niveauNonConformiteService.getAll(pageable);
        return new ResponseEntity<>(niveauNonConformiteDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_NIVEAU_NON_CONFORMITE_BY_ID)
    public ResponseEntity<NiveauNonConformiteDto> getEfficaciteById(@PathVariable UUID id) {
        NiveauNonConformiteDto niveauNonConformiteDto = niveauNonConformiteService.getById(id);
        return new ResponseEntity<>(niveauNonConformiteDto, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_NIVEAU_NON_CONFORMITE)
    public void deleteyId(@PathVariable UUID id) {
        niveauNonConformiteService.delete(id);
    }
}
