package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.ReglementationDto;
import com.qualiapproche.referentiel.service.ReglementationService;
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

@RequestMapping(REGLEMENTATION_ROOT_URL)
@RestController
@RequirePermissions(
        create = {"reglementation-write"},
        update = {"reglementation-write"},
        read = {"reglementation-read", "reglementation-write", "REGLEMENTATION_READ"},
        delete = {"reglementation-write"}
)
public class ReglementationController {

    @Autowired
    private ReglementationService reglementationService;

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'une réglementation                 /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_REGLEMENTATION)
    public ResponseEntity<ReglementationDto> create(@RequestBody ReglementationDto reglementationDto) {
        ReglementationDto reglementation = reglementationService.create(reglementationDto);
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'une réglementation               /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_REGLEMENTATION)
    public ResponseEntity<ReglementationDto> update(@RequestBody ReglementationDto reglementationDto) {
        ReglementationDto reglementation = reglementationService.update(reglementationDto);
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de toutes les réglementations                /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_REGLEMENTATION)
    public ResponseEntity<List<ReglementationDto>> allReglementations() {
        List<ReglementationDto> reglementation = reglementationService.allReglementations();
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'une réglementation par son ID          /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_REGLEMENTATION_BY_ID)
    public ResponseEntity<ReglementationDto> getReglementationById(@PathVariable UUID id) {
        ReglementationDto reglementation = reglementationService.getReglementationById(id);
        return new ResponseEntity<>(reglementation, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'une réglementation             /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_REGLEMENTATION)
    public void deleteById(@PathVariable UUID id) {
        reglementationService.delete(id);
    }
}
