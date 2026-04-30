package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.dto.EfficaciteDto;
import com.qualiapproche.amelioration.service.EfficaciteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(EFFICACITE_ROOT_URL)
public class EfficaciteController {

    private final EfficaciteService efficaciteService;

    @PostMapping(CREATE_EFFICACITE)
    public ResponseEntity<EfficaciteDto> create(@RequestBody EfficaciteDto efficaciteDto) {
        EfficaciteDto efficaciteDto1 = efficaciteService.create(efficaciteDto);
        return new ResponseEntity<>(efficaciteDto1, HttpStatus.OK);
    }

    @PutMapping(UPDATE_EFFICACITE)
    public ResponseEntity<EfficaciteDto> update(@RequestBody EfficaciteDto EfficaciteDto) {
        EfficaciteDto EfficaciteDto1 = efficaciteService.update(EfficaciteDto);
        return new ResponseEntity<>(EfficaciteDto1, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_EFFICACITE)
    public ResponseEntity<List<EfficaciteDto>> allEfficacites() {
        List<EfficaciteDto> efficaciteDtos  = efficaciteService.getAll();
        return new ResponseEntity<>(efficaciteDtos, HttpStatus.OK);
    }
    @GetMapping(GET_EFFICACITE_BY_ID)
    public ResponseEntity<EfficaciteDto> getEfficaciteById(@PathVariable UUID id) {
        EfficaciteDto efficaciteDto  = efficaciteService.getById(id);
        return new ResponseEntity<>(efficaciteDto, HttpStatus.OK);
    }
    @DeleteMapping(DELETE_EFFICACITE)
    public void deleteyId(@PathVariable UUID id) {
        efficaciteService.delete(id);
    }
}
