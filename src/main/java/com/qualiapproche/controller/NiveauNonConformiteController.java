package com.qualiapproche.controller;

import com.qualiapproche.dto.NiveauNonConformiteDto;
import com.qualiapproche.service.NiveauNonConformiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(NIVEAU_NON_CONFORMITE_ROOT_URL)
public class NiveauNonConformiteController {

    private final NiveauNonConformiteService niveauNonConformiteService;

    @PostMapping(CREATE_NIVEAU_NON_CONFORMITE)
    public ResponseEntity<NiveauNonConformiteDto> create(@RequestBody NiveauNonConformiteDto niveauNonConformiteDto) {
        NiveauNonConformiteDto niveauNonConformiteDto1 = niveauNonConformiteService.create(niveauNonConformiteDto);
        return new ResponseEntity<>(niveauNonConformiteDto1, HttpStatus.OK);
    }

    @PutMapping(UPDATE_NIVEAU_NON_CONFORMITE)
    public ResponseEntity<NiveauNonConformiteDto> update(@RequestBody NiveauNonConformiteDto niveauNonConformiteDto) {
        NiveauNonConformiteDto niveauNonConformiteDto1 = niveauNonConformiteService.update(niveauNonConformiteDto);
        return new ResponseEntity<>(niveauNonConformiteDto1, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_NIVEAU_NON_CONFORMITE)
    public ResponseEntity<List<NiveauNonConformiteDto>> allNiveauNonConformite() {
        List<NiveauNonConformiteDto> niveauNonConformiteDtos  = niveauNonConformiteService.getAll();
        return new ResponseEntity<>(niveauNonConformiteDtos, HttpStatus.OK);
    }
    @GetMapping(GET_NIVEAU_NON_CONFORMITE_BY_ID)
    public ResponseEntity<NiveauNonConformiteDto> getEfficaciteById(@PathVariable UUID id) {
        NiveauNonConformiteDto niveauNonConformiteDto  = niveauNonConformiteService.getById(id);
        return new ResponseEntity<>(niveauNonConformiteDto, HttpStatus.OK);
    }
    @DeleteMapping(DELETE_NIVEAU_NON_CONFORMITE)
    public void deleteyId(@PathVariable UUID id) {
        niveauNonConformiteService.delete(id);
    }
}
