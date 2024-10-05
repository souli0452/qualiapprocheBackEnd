package com.qualiapproche.controller;

import com.qualiapproche.dto.CrictereEvaluationDto;
import com.qualiapproche.dto.CritereEvaluationIdsRequestDto;
import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.entities.CrictereEvaluation;
import com.qualiapproche.entities.Fournisseur;
import com.qualiapproche.service.FournisseurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequestMapping(FOURNISSEUR_ROOT_URL)

public class FournisseurController {
    @Autowired
    private  FournisseurService fournisseurService;
    @PostMapping(CREATE_FOURNISSEUR)
    public ResponseEntity<FournisseurDto> create(@RequestBody FournisseurDto fournisseurDto) {
        FournisseurDto fournisseur = fournisseurService.create(fournisseurDto);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }
    @PutMapping(UPDATE_FOURNISSEUR)
    public ResponseEntity<FournisseurDto> update(@RequestBody FournisseurDto fournisseurDto) {
        FournisseurDto fournisseur = fournisseurService.update(fournisseurDto);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_FOURNISSEUR)
    public ResponseEntity<List<FournisseurDto>> allFournissseurs() {
        List<FournisseurDto> fournisseurs = fournisseurService.allFournisseurs();
        return new ResponseEntity<>(fournisseurs, HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<FournisseurDto> getFournisseurById(@RequestParam UUID id) {
      FournisseurDto fournisseur = fournisseurService.getFounisseurById(id);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }


    @PostMapping(ASSIGN_CRICTERE_FOURNISSEUR)
    public List<CrictereEvaluationDto> assignCriteresToFournisseur(@PathVariable UUID fournisseurId, @RequestBody CritereEvaluationIdsRequestDto request) {
        return fournisseurService.assignCriteresToFournisseur(fournisseurId, request.getCritereEvaluationIds());
    }


    @GetMapping(FOURNISSEUR_GET_CRICTERE_EVALUATION)
    public Fournisseur getFournisseurWithCriteres(@PathVariable UUID fournisseurId) {
        return fournisseurService.getFournisseurWithCriteres(fournisseurId);
    }

}
