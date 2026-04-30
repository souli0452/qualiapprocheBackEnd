package com.qualiapproche.referentiel.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.qualiapproche.common.dto.ProduitDto;
import com.qualiapproche.referentiel.service.ProduitService;
import static com.qualiapproche.common.utils.ApiUrls.CREATE_PRODUIT;
import static com.qualiapproche.common.utils.ApiUrls.DELETE_PRODUIT;
import static com.qualiapproche.common.utils.ApiUrls.GET_ALL_PRODUIT;
import static com.qualiapproche.common.utils.ApiUrls.GET_PRODUIT_BY_ID;
import static com.qualiapproche.common.utils.ApiUrls.PRODUIT_ROOT_URL;
import static com.qualiapproche.common.utils.ApiUrls.UPDATE_PRODUIT;

@RequestMapping(PRODUIT_ROOT_URL)
@RestController
public class ProduitController {

    @Autowired
    private ProduitService produitService;

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'un produit                     /
    /-----------------------------------------------------------------------*/
    @PostMapping(CREATE_PRODUIT)
    public ResponseEntity<ProduitDto> create(@RequestBody ProduitDto produitDto) {
        ProduitDto produit = produitService.create(produitDto);
        return new ResponseEntity<>(produit, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'un produit                   /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_PRODUIT)
    public ResponseEntity<ProduitDto> update(@RequestBody ProduitDto produitDto) {
        ProduitDto produit = produitService.update(produitDto);
        return new ResponseEntity<>(produit, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de tous les produits                  /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_ALL_PRODUIT)
    public ResponseEntity<List<ProduitDto>> allProduits() {
        List<ProduitDto> produit = produitService.allProduits();
        return new ResponseEntity<>(produit, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'un produit par son ID              /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_PRODUIT_BY_ID)
    public ResponseEntity<ProduitDto> getProduitById(@PathVariable UUID id) {
        ProduitDto produit = produitService.getProduitById(id);
        return new ResponseEntity<>(produit, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'un produit                 /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_PRODUIT)
    public void deleteById(@PathVariable UUID id) {
        produitService.delete(id);
    }
}
