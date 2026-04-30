package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.ReglementationDto;
import com.qualiapproche.referentiel.service.ReglementationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RequestMapping(REGLEMENTATION_ROOT_URL)
@RestController
public class ReglementationController {

    @Autowired
    private ReglementationService reglementationService;

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'une réglementation                 /
    /-----------------------------------------------------------------------*/
    @PostMapping(CREATE_REGLEMENTATION)
    public ResponseEntity<ReglementationDto> create(@RequestBody ReglementationDto reglementationDto) {
        ReglementationDto reglementation = reglementationService.create(reglementationDto);
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'une réglementation               /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_REGLEMENTATION)
    public ResponseEntity<ReglementationDto> update(@RequestBody ReglementationDto reglementationDto) {
        ReglementationDto reglementation = reglementationService.update(reglementationDto);
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de toutes les réglementations                /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_ALL_REGLEMENTATION)
    public ResponseEntity<List<ReglementationDto>> allReglementations() {
        List<ReglementationDto> reglementation = reglementationService.allReglementations();
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'une réglementation par son ID          /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_REGLEMENTATION_BY_ID)
    public ResponseEntity<ReglementationDto> getReglementationById(@PathVariable UUID id) {
        ReglementationDto reglementation = reglementationService.getReglementationById(id);
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'une réglementation             /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_REGLEMENTATION)
    public void deleteById(@PathVariable UUID id) {
        reglementationService.delete(id);
    }
}
