package com.qualiapproche.controller;

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

import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.service.NonConformiteService;
import static com.qualiapproche.utils.ApiUrls.CREATE_NON_CONFORMITE;
import static com.qualiapproche.utils.ApiUrls.DELETE_NON_CONFORMITE;
import static com.qualiapproche.utils.ApiUrls.GET_ALL_NON_CONFORMITE;
import static com.qualiapproche.utils.ApiUrls.GET_NON_CONFORMITE_BY_ID;
import static com.qualiapproche.utils.ApiUrls.NON_CONFORMITE_ROOT_URL;
import static com.qualiapproche.utils.ApiUrls.UPDATE_NON_CONFORMITE;

@RestController
@RequestMapping(NON_CONFORMITE_ROOT_URL)
public class NonConformiteController {

    @Autowired
    private  NonConformiteService nonConformiteService;

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'une NonConformité                  /
    /-----------------------------------------------------------------------*/
    @PostMapping(CREATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> create(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.create(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'une NonConformite                /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> update(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.update(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de toutes les NonConformités              /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_ALL_NON_CONFORMITE)
    public ResponseEntity<List<NonConformiteDto>> allNonConformites() {
        List<NonConformiteDto> nonConformite = nonConformiteService.allNonConformites();
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'une NonConformité par son ID           /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_NON_CONFORMITE_BY_ID)
    public ResponseEntity<NonConformiteDto> getNonConformiteById(@PathVariable UUID id) {
        NonConformiteDto nonConformite = nonConformiteService.getNonConformiteById(id);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'une NonConformité              /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_NON_CONFORMITE)
    public void deleteById(@PathVariable UUID id) {
        nonConformiteService.delete(id);
    }

}
