package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.CrictereEvaluationDto;
import com.qualiapproche.common.dto.CritereEvaluationIdsRequestDto;
import com.qualiapproche.common.dto.FournisseurDto;
import com.qualiapproche.referentiel.entities.Fournisseur;
import com.qualiapproche.referentiel.service.FournisseurService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequestMapping(FOURNISSEUR_ROOT_URL)

@RequirePermissions(
        create = {"fournisseur-write", "FOURNISSEUR_MANAGE"},
        update = {"fournisseur-write", "FOURNISSEUR_MANAGE"},
        read = {"fournisseur-read", "fournisseur-write", "RESOURCES_READ", "FOURNISSEUR_MANAGE"},
        delete = {"fournisseur-write", "FOURNISSEUR_MANAGE"}
)
public class FournisseurController {
    @Autowired
    private FournisseurService fournisseurService;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_FOURNISSEUR)
    public ResponseEntity<FournisseurDto> create(@RequestBody FournisseurDto fournisseurDto) {
        FournisseurDto fournisseur = fournisseurService.create(fournisseurDto);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_FOURNISSEUR)
    public ResponseEntity<FournisseurDto> update(@RequestBody FournisseurDto fournisseurDto) {
        FournisseurDto fournisseur = fournisseurService.update(fournisseurDto);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_FOURNISSEUR)
    public ResponseEntity<List<FournisseurDto>> allFournissseurs() {
        List<FournisseurDto> fournisseurs = fournisseurService.allFournisseurs();
        return new ResponseEntity<>(fournisseurs, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<FournisseurDto> getFournisseurById(@RequestParam UUID id) {
        FournisseurDto fournisseur = fournisseurService.getFounisseurById(id);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(ASSIGN_CRICTERE_FOURNISSEUR)
    public List<CrictereEvaluationDto> assignCriteresToFournisseur(@PathVariable UUID fournisseurId,
            @RequestBody CritereEvaluationIdsRequestDto request) {
        return fournisseurService.assignCriteresToFournisseur(fournisseurId, request.getCritereEvaluationIds());
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(FOURNISSEUR_GET_CRICTERE_EVALUATION)
    public Fournisseur getFournisseurWithCriteres(@PathVariable UUID fournisseurId) {
        return fournisseurService.getFournisseurWithCriteres(fournisseurId);
    }

      /*--------------------------------------------------------------------------/
     /                    Méthode de suppression d'une fournnisseur              /
    /--------------------------------------------------------------------------*/

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_FOURNISSEUR)
    public void deleteyId(@PathVariable UUID id) {
        fournisseurService.delete(id);

    }

}
