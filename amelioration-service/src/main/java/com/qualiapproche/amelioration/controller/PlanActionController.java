package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.dto.PlanActionDto;

import com.qualiapproche.common.utils.StatutEnum;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.amelioration.service.PlanActionService;
import static com.qualiapproche.common.utils.ApiUrls.*;

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
    @PutMapping(UPDATE_PLAN_ACTION)
    public ResponseEntity<PlanActionDto> update(@RequestBody PlanActionDto dto) throws IOException {
        PlanActionDto planActionDto = planActionService.changeStatus(dto);
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

    @GetMapping(GET_ALL_PLAN_ACTION_RESPONSABLE)
    public ResponseEntity<List<PlanActionDto>> getAllActionsByResponsbale(@PathVariable String email, @PathVariable StatutEnum status) {
        List<PlanActionDto> planActionDtos  = planActionService.planActionByResponsable(email,status);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_PLAN_ACTION_ALL)
    public ResponseEntity<List<PlanActionDto>> getAllActionsByResponsbaleAll(@PathVariable String email ) {
        List<PlanActionDto> planActionDtos  = planActionService.planActionByResponsableAll(email);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @PutMapping(REJET_PLAN_ACTION)
    public ResponseEntity<PlanActionDto> rejet(@RequestBody PlanActionDto dto) throws IOException {
        PlanActionDto planActionDto = planActionService.rejet(dto);
        return new ResponseEntity<>(planActionDto, HttpStatus.OK);
    }
    @GetMapping(path = "/stats/status/{annee}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> statService(@PathVariable int annee) {
        return ResponseEntity.ok(planActionService.getFrequenceTraitementParMois(annee));
    }

}
