package com.qualiapproche.controller;

import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.service.FournisseurService;
import lombok.RequiredArgsConstructor;
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
    @PostMapping
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
}
