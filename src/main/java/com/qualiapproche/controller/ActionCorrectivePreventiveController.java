package com.qualiapproche.controller;
import com.qualiapproche.dto.ActionCorrectivePreventiveDto;
import com.qualiapproche.service.ActionCorrectivePreventiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ACTION_ROOT_URL)
public class ActionCorrectivePreventiveController {
    private final ActionCorrectivePreventiveService actionCorrectivePreventiveService;
    @PostMapping
    public ResponseEntity<ActionCorrectivePreventiveDto> create(@RequestBody ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventiveDto action = actionCorrectivePreventiveService.createAction(actionCorrectivePreventiveDto);
        return new ResponseEntity<>(action, HttpStatus.OK);
    }
    @PutMapping(UPDATE_ACTION)
    public ResponseEntity<ActionCorrectivePreventiveDto> update(@RequestBody ActionCorrectivePreventiveDto actionCorrectivePreventiveDto) {
        ActionCorrectivePreventiveDto action = actionCorrectivePreventiveService.updateAction(actionCorrectivePreventiveDto);
        return new ResponseEntity<>(action, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_ACTION)
    public ResponseEntity<List<ActionCorrectivePreventiveDto>> getAll() {
        List<ActionCorrectivePreventiveDto>  actions = actionCorrectivePreventiveService.getAlls();
        return new ResponseEntity<>(actions, HttpStatus.OK);
    }
    @GetMapping(GET_ACTION_BY_ID)
    public ResponseEntity<ActionCorrectivePreventiveDto> getById(@RequestParam UUID id) {
        ActionCorrectivePreventiveDto action = actionCorrectivePreventiveService.getActionById(id);
        return new ResponseEntity<>(action, HttpStatus.OK);
    }
    @DeleteMapping(DELETE_ACTION)
    public void deleteyId(@RequestParam UUID id) {
        actionCorrectivePreventiveService.deleteAction(id);

    }
}
