package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.PrestataireDto;
import com.qualiapproche.referentiel.service.PrestataireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RequestMapping(PRESTATAIRE_ROOT_URL)
@RestController
public class PrestataireController {

    @Autowired
    private PrestataireService prestataireService;

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'un prestataire                     /
    /-----------------------------------------------------------------------*/
    @PostMapping(CREATE_PRESTATAIRE)
    public ResponseEntity<PrestataireDto> create(@RequestBody PrestataireDto prestataireDto) {
        PrestataireDto prestataire = prestataireService.create(prestataireDto);
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'un prestataire                   /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_PRESTATAIRE)
    public ResponseEntity<PrestataireDto> update(@RequestBody PrestataireDto prestataireDto) {
        PrestataireDto prestataire = prestataireService.update(prestataireDto);
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de tous les prestataires                  /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_ALL_PRESTATAIRE)
    public ResponseEntity<List<PrestataireDto>> allPrestataires() {
        List<PrestataireDto> prestataire = prestataireService.allPrestataires();
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'un prestataire par son ID              /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_PRESTATAIRE_BY_ID)
    public ResponseEntity<PrestataireDto> getPrestataireById(@PathVariable UUID id) {
        PrestataireDto prestataire = prestataireService.getPrestataireById(id);
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'un prestataire                 /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_PRESTATAIRE)
    public void deleteById(@PathVariable UUID id) {
        prestataireService.delete(id);
    }
}
