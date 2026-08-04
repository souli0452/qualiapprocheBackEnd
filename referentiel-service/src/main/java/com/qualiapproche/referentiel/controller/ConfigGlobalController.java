package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequirePermissions(
        create = {"config-global-write", "CONFIG_GLOBAL_MANAGE"},
        update = {"config-global-write", "CONFIG_GLOBAL_MANAGE"},
        read = {"config-global-read", "config-global-write", "CONFIG_READ", "CONFIG_GLOBAL_MANAGE"},
        delete = {"config-global-write", "CONFIG_GLOBAL_MANAGE"}
)
public class ConfigGlobalController {
    private final ConfigGlobalServiceImpl configGlobalService;
    private final ConfigGlobalMapper configGlobalMapper;
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_CG)
    public ResponseEntity<ConfigGlobalDto> create(@RequestBody ConfigGlobalDto configGlobalDto) {
        ConfigGlobal configGlobal = configGlobalMapper.toEntity(configGlobalDto);
        ConfigGlobal saved = configGlobalService.createConfigGlobal(configGlobal);
        return new ResponseEntity<>(configGlobalMapper.toDto(saved), HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_CG)
    public ResponseEntity<ConfigGlobalDto> update(@RequestBody ConfigGlobalDto configGlobalDto, @PathVariable String id) {
        ConfigGlobal configGlobal = configGlobalMapper.toEntity(configGlobalDto);
        ConfigGlobal updated = configGlobalService.updateConfigGlobal(configGlobal, id);
        return new ResponseEntity<>(configGlobalMapper.toDto(updated), HttpStatus.OK);
    }

    // Appelé de service à service (Feign) : la gateway n'intervient pas, aucune
    // permission applicative n'y est résolue. Non gardé, comme les points de
    // pilotage d'instances de WorkflowController.
    @GetMapping(GET_ALL_CG)
    public ResponseEntity<ConfigGlobalDto> get() {
        ConfigGlobal configGlobal = configGlobalService.getConfigGlobal();
        return new ResponseEntity<>(configGlobalMapper.toDto(configGlobal), HttpStatus.OK);
    }
}
