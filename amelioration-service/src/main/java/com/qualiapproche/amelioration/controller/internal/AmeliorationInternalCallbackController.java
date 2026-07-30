package com.qualiapproche.amelioration.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.amelioration.service.PlanActionService;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/callbacks")
@RequiredArgsConstructor
public class AmeliorationInternalCallbackController {

    private final NonConformiteService nonConformiteService;
    private final PlanActionService planActionService;

    @PostMapping("/non-conformites/{ncId}/status")
    public ResponseEntity<Void> updateNonConformiteStatus(
            @PathVariable("ncId") UUID ncId,
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received workflow callback for NC {}: {}", ncId, payload);
        
        String workflowStatusName = (String) payload.get("statusName");
        String etatTraitementName = (String) payload.get("etatCode"); // Le code d'état que la transition a atteint

        nonConformiteService.updateWorkflowState(ncId, workflowStatusName, etatTraitementName);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/plan-actions/{paId}/status")
    public ResponseEntity<Void> updatePlanActionStatus(
            @PathVariable("paId") UUID paId,
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received workflow callback for PlanAction {}: {}", paId, payload);
        
        String workflowStatusName = (String) payload.get("statusName");
        String etatTraitementName = (String) payload.get("etatCode");

        planActionService.updateWorkflowState(paId, workflowStatusName, etatTraitementName);
        
        return ResponseEntity.ok().build();
    }
}
