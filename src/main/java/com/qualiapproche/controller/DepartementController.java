package com.qualiapproche.controller;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.dto.DepartementDto;
import com.qualiapproche.service.DepartementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(DEPARTEMENT_ROOT_URL)
public class DepartementController {

    private final DepartementService departementService;

    /*-----------------------------------------------------------------------/
  /                     Méthode de création d'un département                 /
/--------------------------------------------------------------------------*/

    @PostMapping(CREATE_DEPARTEMENT)
    public ResponseEntity<DepartementDto> create(@RequestBody DepartementDto departementDto) {
        DepartementDto departement = departementService.create(departementDto);
        return new ResponseEntity<>(departement, HttpStatus.CREATED);
    }

        /*------------------------------------------------------------------------/
       /                     Méthode de consultation d'un département par ID      /
      /--------------------------------------------------------------------------*/

    @GetMapping(GET_DEPARTEMENT_BY_ID)
    public ResponseEntity<DepartementDto> getDepartementById(@RequestParam UUID id) {
        DepartementDto departementDto = departementService.getDepartementById(id);
        return new ResponseEntity<>(departementDto, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /                 Méthode de consultation de  tout les département           /
    /--------------------------------------------------------------------------*/

    @GetMapping(GET_ALL_DEPARTEMENT)
    public ResponseEntity<List<DepartementDto>> allDepartement() {
        List<DepartementDto> departements = departementService.getallDepartement();
        return new ResponseEntity<>(departements, HttpStatus.OK);
    }

    /*----------------------------------------------------------------------------/
    /           Méthode de modification d'un département                         /
    /--------------------------------------------------------------------------*/

    @PutMapping(UPDATE_DEPARTEMENT)
    public ResponseEntity<DepartementDto> update(@RequestBody DepartementDto departementDto) {
        DepartementDto departement = departementService.update(departementDto);
        return new ResponseEntity<>(departement, HttpStatus.OK);
    }

      /*--------------------------------------------------------------------------/
     /           Méthode de suppression d'un crictère d'évaluation               /
    /--------------------------------------------------------------------------*/

    @DeleteMapping(DELETE_DEPARTEMENT)
    public void deleteyId(@RequestParam UUID id) {
        departementService.delete(id);
    }
}
