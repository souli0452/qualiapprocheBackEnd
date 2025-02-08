package com.qualiapproche.controller;

import com.qualiapproche.dto.TypeProcessusDto;
import com.qualiapproche.service.TypeNonConformiteService;
import com.qualiapproche.service.TypeProcessusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(TYPE_PROCESSUS_ROOT_URL)
public class TypeProcessusController {

    private final TypeProcessusService typeProcessusService;

    @PostMapping(CREATE_TYPE_PROCESSUS)
    public ResponseEntity<TypeProcessusDto> create(@RequestBody TypeProcessusDto typeProcessusDto) {
        TypeProcessusDto TypeProcessusDto1 = typeProcessusService.create(typeProcessusDto);
        return new ResponseEntity<>(TypeProcessusDto1, HttpStatus.OK);
    }

    @PutMapping(UPDATE_TYPE_PROCESSUS)
    public ResponseEntity<TypeProcessusDto> update(@RequestBody TypeProcessusDto typeProcessusDto) {
        TypeProcessusDto TypeProcessusDto1 = typeProcessusService.update(typeProcessusDto);
        return new ResponseEntity<>(TypeProcessusDto1, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_TYPE_PROCESSUS)
    public ResponseEntity<List<TypeProcessusDto>> allTypeProcessus() {
        List<TypeProcessusDto> typeProcessusDtos  = typeProcessusService.getAll();
        return new ResponseEntity<>(typeProcessusDtos, HttpStatus.OK);
    }
    @GetMapping(GET_TYPE_PROCESSUS_BY_ID)
    public ResponseEntity<TypeProcessusDto> getById(@PathVariable UUID id) {
        TypeProcessusDto typeProcessusDto  = typeProcessusService.getById(id);
        return new ResponseEntity<>(typeProcessusDto, HttpStatus.OK);
    }
    @DeleteMapping(DELETE_TYPE_PROCESSUS)
    public void deleteyId(@PathVariable UUID id) {
        typeProcessusService.delete(id);
    }
}
