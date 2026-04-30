package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.dto.ReclamationDto;
import com.qualiapproche.amelioration.service.ReclamationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RequestMapping(RECLAMATION_ROOT_URL)
@RestController
@Tag(name = "Réclamations", description = "Gestion des réclamations clients et internes")
public class ReclamationController {

    @Autowired
    private ReclamationService reclamationService;

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'une Reclamation               /
    /-----------------------------------------------------------------------*/
    @Operation(summary = "Créer une réclamation", description = "Enregistre une nouvelle réclamation dans le système")
    @PostMapping(CREATE_RECLAMATION)
    public ResponseEntity<ReclamationDto> create(@RequestBody ReclamationDto reclamationDto) {
        ReclamationDto reclamation = reclamationService.create(reclamationDto);
        return new ResponseEntity<>(reclamation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'une Reclamation               /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_RECLAMATION)
    public ResponseEntity<ReclamationDto> update(@RequestBody ReclamationDto reclamationDto) {
        ReclamationDto reclamation = reclamationService.update(reclamationDto);
        return new ResponseEntity<>(reclamation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de toutes les Reclamations                /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_ALL_RECLAMATION)
    public ResponseEntity<List<ReclamationDto>> allReclamations() {
        List<ReclamationDto> reclamation = reclamationService.allReclamations();
        return new ResponseEntity<>(reclamation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'une Reclamation par son ID             /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_RECLAMATION_BY_ID)
    public ResponseEntity<ReclamationDto> getReclamationById(@PathVariable UUID id) {
        ReclamationDto reclamation = reclamationService.getReclamationById(id);
        return new ResponseEntity<>(reclamation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'une Reclamation                /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_RECLAMATION)
    public void deleteById(@PathVariable UUID id) {
        reclamationService.delete(id);
    }
}
