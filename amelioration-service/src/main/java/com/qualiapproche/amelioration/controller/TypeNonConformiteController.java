package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.dto.TypeNonConformiteDto;
import com.qualiapproche.amelioration.service.TypeNonConformiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(TYPE_NON_CONFORMITE_ROOT_URL)
@Tag(name = "Types de Non-Conformité", description = "Paramétrage des catégories de non-conformité")
public class TypeNonConformiteController {

    private final TypeNonConformiteService typeNonConformiteService;

    @Operation(summary = "Créer un type de NC", description = "Définit une nouvelle catégorie de non-conformité")
    @PostMapping(CREATE_TYPE_NON_CONFORMITE)
    public ResponseEntity<TypeNonConformiteDto> create(@RequestBody TypeNonConformiteDto typeNonConformiteDto) {
        TypeNonConformiteDto typeNonConformiteDto1 = typeNonConformiteService.create(typeNonConformiteDto);
        return new ResponseEntity<>(typeNonConformiteDto1, HttpStatus.OK);
    }

    @PutMapping(UPDATE_TYPE_NON_CONFORMITE)
    public ResponseEntity<TypeNonConformiteDto> update(@RequestBody TypeNonConformiteDto typeNonConformiteDto) {
        TypeNonConformiteDto TypeNonConformiteDto1 = typeNonConformiteService.update(typeNonConformiteDto);
        return new ResponseEntity<>(TypeNonConformiteDto1, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_TYPE_NON_CONFORMITE)
    public ResponseEntity<List<TypeNonConformiteDto>> allTypeNonConformite() {
        List<TypeNonConformiteDto> typeNonConformiteDtos  = typeNonConformiteService.getAll();
        return new ResponseEntity<>(typeNonConformiteDtos, HttpStatus.OK);
    }
    @GetMapping(GET_TYPE_NON_CONFORMITE_BY_ID)
    public ResponseEntity<TypeNonConformiteDto> getById(@PathVariable UUID id) {
        TypeNonConformiteDto typeNonConformiteDto  = typeNonConformiteService.getById(id);
        return new ResponseEntity<>(typeNonConformiteDto, HttpStatus.OK);
    }
    @DeleteMapping(DELETE_TYPE_NON_CONFORMITE)
    public void deleteyId(@PathVariable UUID id) {
        typeNonConformiteService.delete(id);
    }
}
