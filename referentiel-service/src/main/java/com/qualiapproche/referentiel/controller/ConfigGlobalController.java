package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.ActionDto;
import com.qualiapproche.referentiel.entities.ConfigGlobal;
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
    @PostMapping(CREATE_CG)
    public ResponseEntity<ConfigGlobal> create(@RequestBody ConfigGlobal configGlobal) {
        ConfigGlobal configGlobal1 = configGlobalService.createConfigGlobal(configGlobal);
        return new ResponseEntity<>(configGlobal, HttpStatus.OK);
    }

    @PutMapping(UPDATE_CG)
    public ResponseEntity<ConfigGlobal> update(@RequestBody ConfigGlobal configGlobal,@PathVariable String id) {
        ConfigGlobal c = configGlobalService.updateConfigGlobal(configGlobal,id);
        return new ResponseEntity<>(c, HttpStatus.OK);
    }

    @GetMapping(GET_ALL_CG)
    public ResponseEntity<ConfigGlobal> get() {
        ConfigGlobal actionDtos  = configGlobalService.getConfigGlobal();
        return new ResponseEntity<>(actionDtos, HttpStatus.OK);
    }
}
