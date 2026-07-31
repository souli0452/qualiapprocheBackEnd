package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.utils.StatutEnum;

import java.io.IOException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;

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

    @GetMapping(GET_ALL_PLAN_ACTION)
    public ResponseEntity<Page<PlanActionDto>> allActions(@ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos  = planActionService.allPlanActions(pageable);
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
    public ResponseEntity<Page<PlanActionDto>> getAllActionsByResponsbale(@PathVariable String email, @PathVariable StatutEnum status, @ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos  = planActionService.planActionByResponsable(email,status, pageable);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @GetMapping(GET_ALL_PLAN_ACTION_ALL)
    public ResponseEntity<Page<PlanActionDto>> getAllActionsByResponsbaleAll(@PathVariable String email, @ParameterObject Pageable pageable ) {
        Page<PlanActionDto> planActionDtos  = planActionService.planActionByResponsableAll(email, pageable);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @GetMapping(path = "/stats/status/{annee}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> statService(@PathVariable int annee) {
        return ResponseEntity.ok(planActionService.getFrequenceTraitementParMois(annee));
    }

    @GetMapping("/structure/{structureId}")
    public ResponseEntity<Page<PlanActionDto>> getPlanActionsByStructure(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos = planActionService.getPlanActionsByStructure(structureId, pageable);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
}
