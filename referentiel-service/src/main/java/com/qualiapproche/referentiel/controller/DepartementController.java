package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.DepartementDto;
import com.qualiapproche.referentiel.service.DepartementService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(DEPARTEMENT_ROOT_URL)
@RequirePermissions(
        create = {"departement-write", "STRUCT_MANAGE"},
        update = {"departement-write", "STRUCT_MANAGE"},
        read = {"departement-read", "departement-write", "CONFIG_READ", "STRUCT_MANAGE"},
        delete = {"departement-write", "STRUCT_MANAGE"}
)
public class DepartementController {

    private final DepartementService departementService;

    /*-----------------------------------------------------------------------/
  /                     Méthode de création d'un département                 /
/--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_DEPARTEMENT)
    public ResponseEntity<DepartementDto> create(@RequestBody DepartementDto departementDto) {
        DepartementDto departement = departementService.create(departementDto);
        return new ResponseEntity<>(departement, HttpStatus.CREATED);
    }

        /*------------------------------------------------------------------------/
       /                     Méthode de consultation d'un département par ID      /
      /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_DEPARTEMENT_BY_ID)
    public ResponseEntity<DepartementDto> getDepartementById(@RequestParam UUID id) {
        DepartementDto departementDto = departementService.getDepartementById(id);
        return new ResponseEntity<>(departementDto, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /                 Méthode de consultation de  tout les département           /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_DEPARTEMENT)
    public ResponseEntity<List<DepartementDto>> allDepartement() {
        List<DepartementDto> departements = departementService.getallDepartement();
        return new ResponseEntity<>(departements, HttpStatus.OK);
    }

    /*----------------------------------------------------------------------------/
    /           Méthode de modification d'un département                         /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_DEPARTEMENT)
    public ResponseEntity<DepartementDto> update(@RequestBody DepartementDto departementDto) {
        DepartementDto departement = departementService.update(departementDto);
        return new ResponseEntity<>(departement, HttpStatus.OK);
    }

      /*--------------------------------------------------------------------------/
     /           Méthode de suppression d'un crictère d'évaluation               /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_DEPARTEMENT)
    public void deleteyId(@PathVariable UUID id) {
        departementService.delete(id);
    }
}
