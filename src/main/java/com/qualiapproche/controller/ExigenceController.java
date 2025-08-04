package com.qualiapproche.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qualiapproche.dto.ExigenceDto;
import com.qualiapproche.service.ExigenceService;
import static com.qualiapproche.utils.ApiUrls.CREATE_EXIGENCE;
import static com.qualiapproche.utils.ApiUrls.DELETE_EXIGENCE;
import static com.qualiapproche.utils.ApiUrls.EXIGENCE_ROOT_URL;
import static com.qualiapproche.utils.ApiUrls.GET_ALL_EXIGENCE;
import static com.qualiapproche.utils.ApiUrls.GET_EXIGENCE_BY_ID;
import static com.qualiapproche.utils.ApiUrls.UPDATE_EXIGENCE;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EXIGENCE_ROOT_URL)
public class ExigenceController {

    private final ExigenceService exigenceService;

    /*-----------------------------------------------------------------------/
  /                     Méthode de création d'une exigence                 /
/--------------------------------------------------------------------------*/

    @PostMapping(CREATE_EXIGENCE)
    public ResponseEntity<ExigenceDto> create(@RequestBody ExigenceDto exigenceDto) {
        ExigenceDto exigence = exigenceService.create(exigenceDto);
        return new ResponseEntity<>(exigence, HttpStatus.CREATED);
    }

        /*------------------------------------------------------------------------/
       /                     Méthode de consultation d'une exigences par ID      /
      /--------------------------------------------------------------------------*/

    @GetMapping(GET_EXIGENCE_BY_ID)
    public ResponseEntity<ExigenceDto> getExigenceById(@PathVariable UUID id) {
        ExigenceDto exigenceDto = exigenceService.getExigenceById(id);
        return new ResponseEntity<>(exigenceDto, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /                 Méthode de consultation de  toutes les exigences           /
    /--------------------------------------------------------------------------*/

    @GetMapping(GET_ALL_EXIGENCE)
    public ResponseEntity<List<ExigenceDto>> allExigence() {
        List<ExigenceDto> exigences = exigenceService.allExigences();
        return new ResponseEntity<>(exigences, HttpStatus.OK);
    }

    /*----------------------------------------------------------------------------/
    /           Méthode de modification d'une exigences                         /
    /--------------------------------------------------------------------------*/

    @PutMapping(UPDATE_EXIGENCE)
    public ResponseEntity<ExigenceDto> update(@RequestBody ExigenceDto exigenceDto) {
        ExigenceDto exigence = exigenceService.update(exigenceDto);
        return new ResponseEntity<>(exigence, HttpStatus.OK);
    }

      /*--------------------------------------------------------------------------/
     /           Méthode de suppression d'une exigence             /
    /--------------------------------------------------------------------------*/

    @DeleteMapping(DELETE_EXIGENCE)
    public void deleteyId(@PathVariable UUID id) {
        exigenceService.delete(id);
    }
}
