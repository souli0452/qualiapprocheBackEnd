package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.PrestataireDto;
import com.qualiapproche.referentiel.service.PrestataireService;
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

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RequestMapping(PRESTATAIRE_ROOT_URL)
@RestController
@RequirePermissions(
        create = {"prestataire-write"},
        update = {"prestataire-write"},
        read = {"prestataire-read", "prestataire-write", "RESOURCES_READ"},
        delete = {"prestataire-write"}
)
public class PrestataireController {

    @Autowired
    private PrestataireService prestataireService;

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'un prestataire                     /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_PRESTATAIRE)
    public ResponseEntity<PrestataireDto> create(@RequestBody PrestataireDto prestataireDto) {
        PrestataireDto prestataire = prestataireService.create(prestataireDto);
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'un prestataire                   /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_PRESTATAIRE)
    public ResponseEntity<PrestataireDto> update(@RequestBody PrestataireDto prestataireDto) {
        PrestataireDto prestataire = prestataireService.update(prestataireDto);
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de tous les prestataires                  /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_PRESTATAIRE)
    public ResponseEntity<List<PrestataireDto>> allPrestataires() {
        List<PrestataireDto> prestataire = prestataireService.allPrestataires();
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'un prestataire par son ID              /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_PRESTATAIRE_BY_ID)
    public ResponseEntity<PrestataireDto> getPrestataireById(@PathVariable UUID id) {
        PrestataireDto prestataire = prestataireService.getPrestataireById(id);
        return new ResponseEntity<>(prestataire, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'un prestataire                 /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_PRESTATAIRE)
    public void deleteById(@PathVariable UUID id) {
        prestataireService.delete(id);
    }
}
