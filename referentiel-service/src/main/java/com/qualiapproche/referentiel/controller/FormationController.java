package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.FormationDto;
import com.qualiapproche.common.dto.FournisseurDto;
import com.qualiapproche.referentiel.service.FormationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(FORMATION_ROOT_URL)
public class FormationController {


    private final FormationService formationService;


    @PostMapping(CREATE_FORMATION)
    public ResponseEntity<FormationDto> create(@RequestBody FormationDto formationDto) {
        FormationDto formationDto1 = formationService.create(formationDto);
        return new ResponseEntity<>(formationDto1, HttpStatus.OK);
    }
    @PutMapping(UPDATE_FORMATION)
    public ResponseEntity<FormationDto> update(@RequestBody FormationDto formationDto) {
        FormationDto formationDto1 = formationService.update(formationDto);
        return new ResponseEntity<>(formationDto1, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_FORMATION)
    public ResponseEntity<List<FormationDto>> allFormations() {
        List<FormationDto> formationDtos = formationService.getAll();
        return new ResponseEntity<>(formationDtos, HttpStatus.OK);
    }
    @GetMapping(GET_FORMATION_BY_ID)
    public ResponseEntity<FormationDto> getFournisseurById(@PathVariable UUID id) {
        FormationDto formationDto = formationService.getById(id);
        return new ResponseEntity<>(formationDto, HttpStatus.OK);
    }
    @DeleteMapping(DELETE_FORMATION)
    public void deleteyId(@PathVariable UUID id) {
        formationService.delete(id);
    }
}
