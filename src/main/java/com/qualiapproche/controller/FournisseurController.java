package com.qualiapproche.controller;

import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.service.FournisseurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequestMapping(FOURNISSEUR_ROOT_URL)
@RequiredArgsConstructor
public class FournisseurController {
    private  final FournisseurService fournisseurService;
    @PostMapping
    public ResponseEntity<FournisseurDto> create(@RequestBody FournisseurDto fournisseurDto) {
        FournisseurDto fournisseur = fournisseurService.create(fournisseurDto);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }
    @PutMapping
    public ResponseEntity<FournisseurDto> update(@RequestBody FournisseurDto fournisseurDto) {
        FournisseurDto fournisseur = fournisseurService.update(fournisseurDto);
        return new ResponseEntity<>(fournisseur, HttpStatus.OK);
    }
    @GetMapping
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
