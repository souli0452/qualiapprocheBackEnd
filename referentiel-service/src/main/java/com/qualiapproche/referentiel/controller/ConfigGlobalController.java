package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.ConfigGlobalDto;
import com.qualiapproche.referentiel.entities.ConfigGlobal;
import com.qualiapproche.referentiel.entities.mappers.ConfigGlobalMapper;
import com.qualiapproche.referentiel.service.CategorieFichierService;
import com.qualiapproche.referentiel.service.impl.ConfigGlobalServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(CG_ROOT_URL)
public class ConfigGlobalController {
    private final ConfigGlobalServiceImpl configGlobalService;
    private final ConfigGlobalMapper configGlobalMapper;
    @PostMapping(CREATE_CG)
    public ResponseEntity<ConfigGlobalDto> create(@RequestBody ConfigGlobalDto configGlobalDto) {
        ConfigGlobal configGlobal = configGlobalMapper.toEntity(configGlobalDto);
        ConfigGlobal saved = configGlobalService.createConfigGlobal(configGlobal);
        return new ResponseEntity<>(configGlobalMapper.toDto(saved), HttpStatus.OK);
    }

    @PutMapping(UPDATE_CG)
    public ResponseEntity<ConfigGlobalDto> update(@RequestBody ConfigGlobalDto configGlobalDto, @PathVariable String id) {
        ConfigGlobal configGlobal = configGlobalMapper.toEntity(configGlobalDto);
        ConfigGlobal updated = configGlobalService.updateConfigGlobal(configGlobal, id);
        return new ResponseEntity<>(configGlobalMapper.toDto(updated), HttpStatus.OK);
    }

    @GetMapping(GET_ALL_CG)
    public ResponseEntity<ConfigGlobalDto> get() {
        ConfigGlobal configGlobal = configGlobalService.getConfigGlobal();
        return new ResponseEntity<>(configGlobalMapper.toDto(configGlobal), HttpStatus.OK);
    }
}
