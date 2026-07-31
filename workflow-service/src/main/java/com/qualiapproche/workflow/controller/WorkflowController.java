package com.qualiapproche.workflow.controller;

import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import com.qualiapproche.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<List<com.qualiapproche.workflow.dto.WorkflowDto>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @PostMapping
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowDto> createWorkflow(@RequestBody com.qualiapproche.workflow.dto.WorkflowDto workflowDto) {
        return ResponseEntity.ok(workflowService.createWorkflow(workflowDto));
    }

    @PutMapping("/{workflowId}")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowDto> updateWorkflow(@PathVariable UUID workflowId, @RequestBody com.qualiapproche.workflow.dto.WorkflowDto workflowDto) {
        return ResponseEntity.ok(workflowService.updateWorkflow(workflowId, workflowDto));
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID workflowId) {
        workflowService.deleteWorkflow(workflowId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<List<com.qualiapproche.workflow.model.Workflow>> getWorkflowsByType(@PathVariable String documentType) {
        return ResponseEntity.ok(workflowService.getWorkflowsByType(documentType));
    }

    /**
     * Sélection déterministe du workflow actif pour un type : à utiliser à la place de
     * {@code /type/{documentType}} + "premier élément" côté services appelants.
     */
    @GetMapping("/type/{documentType}/active")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowDto> getActiveWorkflowByType(@PathVariable String documentType) {
        return ResponseEntity.ok(workflowService.getActiveWorkflowByType(documentType));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<com.qualiapproche.workflow.model.Workflow> getWorkflowById(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(workflowService.getWorkflowById(workflowId));
    }

    @GetMapping("/instances/{resourceId}")
    public ResponseEntity<WorkflowInstanceDto> getLastValidationInstance(@PathVariable UUID resourceId) {
        WorkflowInstanceDto instance = workflowService.getLastValidationInstance(resourceId);
        return instance != null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
    }

    @GetMapping("/instances/{resourceId}/state")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowStateDto> getWorkflowState(@PathVariable UUID resourceId) {
        com.qualiapproche.workflow.dto.WorkflowStateDto state = workflowService.getWorkflowStateForResource(resourceId);
        if (state == null) {
            state = com.qualiapproche.workflow.dto.WorkflowStateDto.builder()
                    .allowedActions(java.util.Collections.emptyList())
                    .build();
        }
        return ResponseEntity.ok(state);
    }

    @PostMapping("/initiate")
    public ResponseEntity<WorkflowInstanceDto> initiateWorkflow(
            @RequestParam("resourceId") UUID resourceId,
            @RequestParam("resourceType") String resourceType,
            @RequestParam("workflowId") UUID workflowId) {

        return ResponseEntity.ok(workflowService.initiateWorkflow(resourceId, resourceType, workflowId));
    }

    @PostMapping("/validate/{resourceId}")
    public ResponseEntity<Void> validateStep(
            @PathVariable UUID resourceId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody(required = false) WorkflowValidationRequestDto request) {
        
        workflowService.validateStep(resourceId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject/{resourceId}")
    public ResponseEntity<Void> rejectStep(
            @PathVariable UUID resourceId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody(required = false) WorkflowValidationRequestDto request) {
        
        workflowService.rejectStep(resourceId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/execute/{resourceId}")
    public ResponseEntity<Void> executeTransition(
            @PathVariable UUID resourceId,
            @RequestParam("transitionCode") String transitionCode,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody(required = false) WorkflowValidationRequestDto request) {
        
        workflowService.executeDynamicTransition(resourceId, userId, transitionCode, request);
        return ResponseEntity.ok().build();
    }
}
