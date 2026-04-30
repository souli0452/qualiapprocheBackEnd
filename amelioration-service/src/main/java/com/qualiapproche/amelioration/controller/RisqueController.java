package com.qualiapproche.amelioration.controller;

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

import com.qualiapproche.common.dto.RisqueDto;
import com.qualiapproche.amelioration.service.RisqueService;
import static com.qualiapproche.common.utils.ApiUrls.CREATE_RISQUE;
import static com.qualiapproche.common.utils.ApiUrls.DELETE_RISQUE;
import static com.qualiapproche.common.utils.ApiUrls.GET_ALL_RISQUE;
import static com.qualiapproche.common.utils.ApiUrls.GET_RISQUE_BY_ID;
import static com.qualiapproche.common.utils.ApiUrls.RISQUE_ROOT_URL;
import static com.qualiapproche.common.utils.ApiUrls.UPDATE_RISQUE;

@RestController
@RequestMapping(RISQUE_ROOT_URL)
public class RisqueController {

    @Autowired
    private  RisqueService risqueService;

    /*-----------------------------------------------------------------------/
    /                     Méthode de création d'un RISQUE                    /
    /-----------------------------------------------------------------------*/
    @PostMapping(CREATE_RISQUE)
    public ResponseEntity<RisqueDto> create(@RequestBody RisqueDto risqueDto) {
        RisqueDto risque = risqueService.create(risqueDto);
        return new ResponseEntity<>(risque, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                     Méthode de mise à jour d'un RISQUE                 /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_RISQUE)
    public ResponseEntity<RisqueDto> update(@RequestBody RisqueDto risqueDto) {
        RisqueDto risque = risqueService.update(risqueDto);
        return new ResponseEntity<>(risque, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /               Méthode de récupération de tous les RISQUES              /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_ALL_RISQUE)
    public ResponseEntity<List<RisqueDto>> allRisques() {
        List<RisqueDto> risque = risqueService.allRisques();
        return new ResponseEntity<>(risque, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /           Méthode de récupération d'un RISQUE par son ID               /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_RISQUE_BY_ID)
    public ResponseEntity<RisqueDto> getRisqueById(@PathVariable UUID risqueId) {
        RisqueDto risque = risqueService.getRisqueById(risqueId);
        return new ResponseEntity<>(risque, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                  Méthode de suppression d'un RISQUE                    /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_RISQUE)
    public void deleteById(@PathVariable UUID risqueId) {
        risqueService.delete(risqueId);
    }

}
