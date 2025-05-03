package com.qualiapproche.controller;

import com.qualiapproche.dto.TypeNonConformiteDto;
import com.qualiapproche.service.TypeNonConformiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(TYPE_NON_CONFORMITE_ROOT_URL)
public class TypeNonConformiteController {

    private final TypeNonConformiteService typeNonConformiteService;

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
