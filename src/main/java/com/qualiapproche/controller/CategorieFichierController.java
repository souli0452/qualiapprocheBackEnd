package com.qualiapproche.controller;

import com.qualiapproche.dto.CategorieFichierDto;
import com.qualiapproche.service.CategorieFichierService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequestMapping(CATEGORIE_FICHIER_ROOT_URL)
@RequiredArgsConstructor
public class CategorieFichierController {

    private final CategorieFichierService categorieFichierService;

    /*----------------------------------------------------------------------------/
    /                   Méthode de création d'une catégorie                       /
    /----------------------------------------------------------------------------*/

    @PostMapping(CREATE_CATEGORIE_FICHIER)
    public ResponseEntity<CategorieFichierDto> create(@RequestBody CategorieFichierDto categorieFichierDto) {
        CategorieFichierDto categorieFichier = categorieFichierService.create(categorieFichierDto);
        return new ResponseEntity<>(categorieFichier, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /               Méthode de modification d'une catégorie                       /
    /----------------------------------------------------------------------------*/
    @PutMapping(UPDATE_CATEGORIE_FICHIER)
    public ResponseEntity<CategorieFichierDto> update(@RequestBody CategorieFichierDto categorieFichierDto) {
        CategorieFichierDto fournisseurDto = categorieFichierService.update(categorieFichierDto);
        return new ResponseEntity<>(fournisseurDto, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /           Méthode de consultation de  toutes les catégories                /
    /--------------------------------------------------------------------------*/

    @GetMapping(GET_ALL_CATEGORIE_FICHIER)
    public ResponseEntity<List<CategorieFichierDto>> allCategorieFichier() {
        List<CategorieFichierDto> categorieFichiers = categorieFichierService.allCategorieFichier();
        return new ResponseEntity<>(categorieFichiers, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /           Méthode de récupperation d'une catégorie par son ID               /
    /----------------------------------------------------------------------------*/

    @GetMapping()
    public ResponseEntity<CategorieFichierDto> getCategorieFichierById(@RequestParam UUID categorieId) {
        CategorieFichierDto categorieFichier = categorieFichierService.getCategorieFichierById(categorieId);
        return new ResponseEntity<>(categorieFichier, HttpStatus.OK);
    }
}
