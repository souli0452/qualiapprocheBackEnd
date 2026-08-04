package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.ActionDto;
import com.qualiapproche.amelioration.service.ActionService;
import com.qualiapproche.amelioration.service.EfficaciteService;
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
@RequestMapping(ACTIONS_ROOT_URL)
@Tag(name = "Actions", description = "Gestion des actions correctives et préventives")
@RequirePermissions(
        create = {"action-write"},
        update = {"action-write"},
        read = {"action-read", "action-write", "ACTIONS_READ"},
        delete = {"action-write"}
)
public class ActionController {

    private final ActionService actionService;

    @Operation(summary = "Créer une action", description = "Enregistre une nouvelle action dans le référentiel")
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_ACTIONS)
    public ResponseEntity<ActionDto> create(@RequestBody ActionDto actionDto) {
        ActionDto actionDto1 = actionService.create(actionDto);
        return new ResponseEntity<>(actionDto1, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_ACTIONS)
    public ResponseEntity<ActionDto> update(@RequestBody ActionDto actionDto) {
        ActionDto ActionDto1 = actionService.update(actionDto);
        return new ResponseEntity<>(ActionDto1, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_ACTIONS)
    public ResponseEntity<List<ActionDto>> allActions() {
        List<ActionDto> actionDtos  = actionService.getAll();
        return new ResponseEntity<>(actionDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ACTIONS_BY_ID)
    public ResponseEntity<ActionDto> getById(@PathVariable UUID id) {
        ActionDto actionDto  = actionService.getById(id);
        return new ResponseEntity<>(actionDto, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_ACTIONS)
    public void deleteyId(@PathVariable UUID id) {
        actionService.delete(id);
    }
}
