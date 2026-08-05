package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.FormationDto;
import com.qualiapproche.referentiel.service.FormationService;
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

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(FORMATION_ROOT_URL)
@RequirePermissions(
        create = {"formation-write", "FORMATION_MANAGE"},
        update = {"formation-write", "FORMATION_MANAGE"},
        read = {"formation-read", "formation-write", "RESOURCES_READ", "FORMATION_MANAGE"},
        delete = {"formation-write", "FORMATION_MANAGE"}
)
public class FormationController {


    private final FormationService formationService;


    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_FORMATION)
    public ResponseEntity<FormationDto> create(@RequestBody FormationDto formationDto) {
        FormationDto formationDto1 = formationService.create(formationDto);
        return new ResponseEntity<>(formationDto1, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_FORMATION)
    public ResponseEntity<FormationDto> update(@RequestBody FormationDto formationDto) {
        FormationDto formationDto1 = formationService.update(formationDto);
        return new ResponseEntity<>(formationDto1, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_FORMATION)
    public ResponseEntity<List<FormationDto>> allFormations() {
        List<FormationDto> formationDtos = formationService.getAll();
        return new ResponseEntity<>(formationDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_FORMATION_BY_ID)
    public ResponseEntity<FormationDto> getFournisseurById(@PathVariable UUID id) {
        FormationDto formationDto = formationService.getById(id);
        return new ResponseEntity<>(formationDto, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_FORMATION)
    public void deleteyId(@PathVariable UUID id) {
        formationService.delete(id);
    }
}
