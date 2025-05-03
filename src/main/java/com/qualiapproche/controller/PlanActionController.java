package com.qualiapproche.controller;

import com.qualiapproche.dto.PlanActionDto;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.service.PlanActionService;
import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(PLAN_ACTION_ROOT_URL)
public class PlanActionController {

    private final PlanActionService planActionService;

    @PostMapping(CREATE_PLAN_ACTION)
    public ResponseEntity<PlanActionDto> create(@RequestBody PlanActionDto dto) throws IOException {
        PlanActionDto planActionDto = planActionService.createPlanActionDto(dto);
        return new ResponseEntity<>(planActionDto, HttpStatus.OK);
    }

    @GetMapping(GET_ALL_PLAN_ACTION)
    public ResponseEntity<List<PlanActionDto>> allActions() {
        List<PlanActionDto> planActionDtos  = planActionService.allPlanActions();
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @GetMapping(GET_PLAN_ACTION_BY_ID)
    public ResponseEntity<PlanActionDto> getById(@PathVariable UUID id) {
        PlanActionDto planActionDto  = planActionService.getPlanActionDtoById(id);
        return new ResponseEntity<>(planActionDto, HttpStatus.OK);
    }
    @DeleteMapping(DELETE_PLAN_ACTION)
    public void deleteyId(@PathVariable UUID id) {
        planActionService.delete(id);
    }
}
