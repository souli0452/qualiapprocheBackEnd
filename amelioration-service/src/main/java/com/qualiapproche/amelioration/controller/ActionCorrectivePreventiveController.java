package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.ActionCorrectivePreventiveDto;
import com.qualiapproche.amelioration.service.ActionCorrectivePreventiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ACTION_ROOT_URL)
@RequirePermissions(
        create = {"action-corrective-write"},
        update = {"action-corrective-write"},
        read = {"action-corrective-read", "action-corrective-write", "ACTIONS_READ"},
        delete = {"action-corrective-write"}
)
public class ActionCorrectivePreventiveController {
    private final ActionCorrectivePreventiveService actionCorrectivePreventiveService;


    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_ACTION)
    public ResponseEntity<ActionCorrectivePreventiveDto> create(@RequestBody ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventiveDto action = actionCorrectivePreventiveService.createAction(actionCorrectivePreventiveDto);
        return new ResponseEntity<>(action, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_ACTION)
    public ResponseEntity<ActionCorrectivePreventiveDto> update(@RequestBody ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventiveDto action = actionCorrectivePreventiveService.updateAction(actionCorrectivePreventiveDto);
        return new ResponseEntity<>(action, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_ACTION)
    public ResponseEntity<List<ActionCorrectivePreventiveDto>> getAll() {
        List<ActionCorrectivePreventiveDto>  actions = actionCorrectivePreventiveService.getAlls();
        return new ResponseEntity<>(actions, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ACTION_BY_ID)
    public ResponseEntity<ActionCorrectivePreventiveDto> getById(@PathVariable UUID id) {
        ActionCorrectivePreventiveDto action = actionCorrectivePreventiveService.getActionById(id);
        return new ResponseEntity<>(action, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_ACTION)
    public void deleteyId(@PathVariable UUID id) {
        actionCorrectivePreventiveService.deleteAction(id);

    }
}
