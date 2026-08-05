package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.UUID;

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

import com.qualiapproche.common.dto.ExigenceDto;
import com.qualiapproche.referentiel.service.ExigenceService;
import static com.qualiapproche.common.utils.ApiUrls.CREATE_EXIGENCE;
import static com.qualiapproche.common.utils.ApiUrls.DELETE_EXIGENCE;
import static com.qualiapproche.common.utils.ApiUrls.EXIGENCE_ROOT_URL;
import static com.qualiapproche.common.utils.ApiUrls.GET_ALL_EXIGENCE;
import static com.qualiapproche.common.utils.ApiUrls.GET_EXIGENCE_BY_ID;
import static com.qualiapproche.common.utils.ApiUrls.UPDATE_EXIGENCE;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EXIGENCE_ROOT_URL)
@RequirePermissions(
        create = {"exigence-write"},
        update = {"exigence-write"},
        read = {"exigence-read", "exigence-write", "CRITERE_EVAL_READ"},
        delete = {"exigence-write"}
)
public class ExigenceController {

    private final ExigenceService exigenceService;

    /*-----------------------------------------------------------------------/
  /                     Méthode de création d'une exigence                 /
/--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_EXIGENCE)
    public ResponseEntity<ExigenceDto> create(@RequestBody ExigenceDto exigenceDto) {
        ExigenceDto exigence = exigenceService.create(exigenceDto);
        return new ResponseEntity<>(exigence, HttpStatus.CREATED);
    }

        /*------------------------------------------------------------------------/
       /                     Méthode de consultation d'une exigences par ID      /
      /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_EXIGENCE_BY_ID)
    public ResponseEntity<ExigenceDto> getExigenceById(@PathVariable UUID id) {
        ExigenceDto exigenceDto = exigenceService.getExigenceById(id);
        return new ResponseEntity<>(exigenceDto, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /                 Méthode de consultation de  toutes les exigences           /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_EXIGENCE)
    public ResponseEntity<List<ExigenceDto>> allExigence() {
        List<ExigenceDto> exigences = exigenceService.allExigences();
        return new ResponseEntity<>(exigences, HttpStatus.OK);
    }

    /*----------------------------------------------------------------------------/
    /           Méthode de modification d'une exigences                         /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_EXIGENCE)
    public ResponseEntity<ExigenceDto> update(@RequestBody ExigenceDto exigenceDto) {
        ExigenceDto exigence = exigenceService.update(exigenceDto);
        return new ResponseEntity<>(exigence, HttpStatus.OK);
    }

      /*--------------------------------------------------------------------------/
     /           Méthode de suppression d'une exigence             /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_EXIGENCE)
    public void deleteyId(@PathVariable UUID id) {
        exigenceService.delete(id);
    }
}
