package com.qualiapproche.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qualiapproche.dto.AuditeDto;
import com.qualiapproche.service.AuditeService;
import static com.qualiapproche.utils.ApiUrls.AUDITE_ROOT_URL;
import static com.qualiapproche.utils.ApiUrls.CREATE_AUDITE;
import static com.qualiapproche.utils.ApiUrls.DELETE_AUDITE;
import static com.qualiapproche.utils.ApiUrls.GET_ALL_AUDITE;
import static com.qualiapproche.utils.ApiUrls.GET_AUDITE_BY_ID;
import static com.qualiapproche.utils.ApiUrls.UPDATE_AUDITE;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(AUDITE_ROOT_URL)
public class AuditeController {

    private final AuditeService auditeService;

    /*-----------------------------------------------------------------------/
  /                     Méthode de création d'un audite                 /
/--------------------------------------------------------------------------*/

    @PostMapping(CREATE_AUDITE)
    public ResponseEntity<AuditeDto> create(@RequestBody AuditeDto auditeDto) {
        AuditeDto audite = auditeService.create(auditeDto);
        return new ResponseEntity<>(audite, HttpStatus.CREATED);
    }

        /*------------------------------------------------------------------------/
       /                     Méthode de consultation d'un audite par ID      /
      /--------------------------------------------------------------------------*/

    @GetMapping(GET_AUDITE_BY_ID)
    public ResponseEntity<AuditeDto> getAuditeById(@RequestParam UUID id) {
        AuditeDto auditeDto = auditeService.getAuditeById(id);
        return new ResponseEntity<>(auditeDto, HttpStatus.OK);
    }


    /*----------------------------------------------------------------------------/
    /                 Méthode de consultation de  tout les audites               /
    /--------------------------------------------------------------------------*/

    @GetMapping(GET_ALL_AUDITE)
    public ResponseEntity<List<AuditeDto>> allAudite() {
        List<AuditeDto> audites = auditeService.allAudite();
        return new ResponseEntity<>(audites, HttpStatus.OK);
    }

    /*----------------------------------------------------------------------------/
    /           Méthode de modification d'un audite                         /
    /--------------------------------------------------------------------------*/

    @PutMapping(UPDATE_AUDITE)
    public ResponseEntity<AuditeDto> update(@RequestBody AuditeDto auditeDto) {
        AuditeDto audite = auditeService.update(auditeDto);
        return new ResponseEntity<>(audite, HttpStatus.OK);
    }

      /*--------------------------------------------------------------------------/
     /           Méthode de suppression d'un audite            /
    /--------------------------------------------------------------------------*/

    @DeleteMapping(DELETE_AUDITE)
    public void deleteyId(@PathVariable UUID id) {
        auditeService.delete(id);
    }
}
