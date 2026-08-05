package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.CategorieFichierDto;
import com.qualiapproche.referentiel.service.CategorieFichierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping(CATEGORIE_FICHIER_ROOT_URL)
@RequiredArgsConstructor
@RequirePermissions(
        create = {"categorie-fichier-write", "DOC_CAT_MANAGE"},
        update = {"categorie-fichier-write", "DOC_CAT_MANAGE"},
        read = {"categorie-fichier-read", "categorie-fichier-write", "DOC_READ", "DOC_CAT_MANAGE"},
        delete = {"categorie-fichier-write", "DOC_CAT_MANAGE"}
)
public class CategorieFichierController {

    private final CategorieFichierService categorieFichierService;

    /*----------------------------------------------------------------------------/
    /                   Méthode de création d'une catégorie                       /
    /----------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_CATEGORIE_FICHIER)
    public ResponseEntity<CategorieFichierDto> create(@RequestBody CategorieFichierDto categorieFichierDto) {
        CategorieFichierDto categorieFichier = categorieFichierService.create(categorieFichierDto);
        return new ResponseEntity<>(categorieFichier, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /               Méthode de modification d'une catégorie                       /
    /----------------------------------------------------------------------------*/
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_CATEGORIE_FICHIER)
    public ResponseEntity<CategorieFichierDto> update(@RequestBody CategorieFichierDto categorieFichierDto) {
        CategorieFichierDto fournisseurDto = categorieFichierService.update(categorieFichierDto);
        return new ResponseEntity<>(fournisseurDto, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /           Méthode de consultation de  toutes les catégories                /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_CATEGORIE_FICHIER)
    public ResponseEntity<List<CategorieFichierDto>> allCategorieFichier() {
        List<CategorieFichierDto> categorieFichiers = categorieFichierService.allCategorieFichier();
        return new ResponseEntity<>(categorieFichiers, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /           Méthode de récupperation d'une catégorie par son ID               /
    /----------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping()
    public ResponseEntity<CategorieFichierDto> getCategorieFichierById(@RequestParam UUID categorieId) {
        CategorieFichierDto categorieFichier = categorieFichierService.getCategorieFichierById(categorieId);
        return new ResponseEntity<>(categorieFichier, HttpStatus.OK);
    }
}
