package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.TypeProcessusDto;
import com.qualiapproche.referentiel.service.TypeProcessusService;
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
@RequestMapping(TYPE_PROCESSUS_ROOT_URL)
@RequirePermissions(
        create = {"type-processus-write", "TYPE_PROC_MANAGE"},
        update = {"type-processus-write", "TYPE_PROC_MANAGE"},
        read = {"type-processus-read", "type-processus-write", "CONFIG_READ", "TYPE_PROC_MANAGE"},
        delete = {"type-processus-write", "TYPE_PROC_MANAGE"}
)
public class TypeProcessusController {

    private final TypeProcessusService typeProcessusService;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_TYPE_PROCESSUS)
    public ResponseEntity<TypeProcessusDto> create(@RequestBody TypeProcessusDto typeProcessusDto) {
        TypeProcessusDto typeProcessusDto1 = typeProcessusService.create(typeProcessusDto);
        return new ResponseEntity<>(typeProcessusDto1, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_TYPE_PROCESSUS)
    public ResponseEntity<TypeProcessusDto> update(@RequestBody TypeProcessusDto typeProcessusDto) {
        TypeProcessusDto typeProcessusDto1 = typeProcessusService.update(typeProcessusDto);
        return new ResponseEntity<>(typeProcessusDto1, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_TYPE_PROCESSUS)
    public ResponseEntity<Page<TypeProcessusDto>> allTypeProcessus(@ParameterObject Pageable pageable) {
        Page<TypeProcessusDto> typeProcessusDtos = typeProcessusService.getAll(pageable);
        return new ResponseEntity<>(typeProcessusDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_TYPE_PROCESSUS_BY_ID)
    public ResponseEntity<TypeProcessusDto> getById(@PathVariable UUID id) {
        TypeProcessusDto typeProcessusDto = typeProcessusService.getById(id);
        return new ResponseEntity<>(typeProcessusDto, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_TYPE_PROCESSUS)
    public void deleteyId(@PathVariable UUID id) {
        typeProcessusService.delete(id);
    }
}
