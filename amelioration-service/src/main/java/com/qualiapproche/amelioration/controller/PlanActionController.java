package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
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
@lombok.extern.slf4j.Slf4j
@RequestMapping(PLAN_ACTION_ROOT_URL)
@RequirePermissions(
        create = {"plan-action-write", "TRAITEMENT_PLAN"},
        update = {"plan-action-write", "TRAITEMENT_PLAN"},
        read = {"plan-action-read", "plan-action-write", "ACTIONS_READ", "TRAITEMENT_PLAN"},
        delete = {"plan-action-write", "TRAITEMENT_PLAN"},
        validate = {"plan-action-validate", "TRAITEMENT_PLAN"}
)
public class PlanActionController {

    private final PlanActionService planActionService;
    private final com.qualiapproche.amelioration.client.WorkflowClient workflowClient;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_PLAN_ACTION)
    public ResponseEntity<PlanActionDto> create(@RequestBody PlanActionDto dto) throws IOException {
        PlanActionDto planActionDto = planActionService.createPlanActionDto(dto);
        return new ResponseEntity<>(planActionDto, HttpStatus.OK);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_PLAN_ACTION)
    public ResponseEntity<Page<PlanActionDto>> allActions(@ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos  = planActionService.allPlanActions(pageable);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_PLAN_ACTION_BY_ID)
    public ResponseEntity<PlanActionDto> getById(@PathVariable UUID id) {
        PlanActionDto planActionDto  = planActionService.getPlanActionDtoById(id);
        planActionDto.setWorkflowState(etatWorkflow(id));
        return new ResponseEntity<>(planActionDto, HttpStatus.OK);
    }

    /**
     * Étape courante et actions autorisées du circuit de validation. Une indisponibilité du
     * service de workflow ne doit pas empêcher la consultation du plan d'action.
     */
    private com.qualiapproche.common.dto.WorkflowStateDto etatWorkflow(UUID resourceId) {
        try {
            return workflowClient.getWorkflowState(resourceId);
        } catch (Exception e) {
            log.warn("État du workflow indisponible pour le plan d'action {} : {}", resourceId, e.getMessage());
            return null;
        }
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_PLAN_ACTION)
    public void deleteyId(@PathVariable UUID id) {
        planActionService.delete(id);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_PLAN_ACTION_RESPONSABLE)
    public ResponseEntity<Page<PlanActionDto>> getAllActionsByResponsbale(@PathVariable String email, @PathVariable StatutEnum status, @ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos  = planActionService.planActionByResponsable(email,status, pageable);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_PLAN_ACTION_ALL)
    public ResponseEntity<Page<PlanActionDto>> getAllActionsByResponsbaleAll(@PathVariable String email, @ParameterObject Pageable pageable ) {
        Page<PlanActionDto> planActionDtos  = planActionService.planActionByResponsableAll(email, pageable);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/status/{annee}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> statService(@PathVariable int annee) {
        return ResponseEntity.ok(planActionService.getFrequenceTraitementParMois(annee));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/structure/{structureId}")
    public ResponseEntity<Page<PlanActionDto>> getPlanActionsByStructure(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos = planActionService.getPlanActionsByStructure(structureId, pageable);
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
}
