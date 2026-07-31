package com.qualiapproche.support.client;

import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowStateDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

import com.qualiapproche.support.config.FeignConfig;

@FeignClient(name = "workflow-service", configuration = FeignConfig.class)
public interface WorkflowClient {

    /**
     * Sélection déterministe du workflow actif pour un type de ressource : remplace
     * l'ancien pattern "prendre le premier élément de la liste" (ordre non garanti).
     */
    @GetMapping("/api/v1/workflows/type/{resourceType}/active")
    WorkflowSummaryDto getActiveWorkflowByType(@PathVariable("resourceType") String resourceType);

    @GetMapping("/api/v1/workflows/{workflowId}")
    Map<String, Object> getWorkflowById(@PathVariable("workflowId") UUID workflowId);

    @PostMapping("/api/v1/workflows/initiate")
    WorkflowInstanceDto initiateWorkflow(@RequestParam("resourceId") UUID resourceId,
                                         @RequestParam("resourceType") String resourceType,
                                         @RequestParam("workflowId") UUID workflowId);

    @GetMapping("/api/v1/workflows/instances/{resourceId}")
    WorkflowInstanceDto getLastValidationInstance(@PathVariable("resourceId") UUID resourceId);

    @GetMapping("/api/v1/workflows/instances/{resourceId}/state")
    WorkflowStateDto getWorkflowState(@PathVariable("resourceId") UUID resourceId);

    @PostMapping("/api/v1/workflows/validate/{resourceId}")
    void validateStep(@PathVariable("resourceId") UUID resourceId,
                      @RequestHeader(value = "X-User-Id", required = false) String userId,
                      @RequestBody(required = false) WorkflowValidationRequestDto request);

    @PostMapping("/api/v1/workflows/reject/{resourceId}")
    void rejectStep(@PathVariable("resourceId") UUID resourceId,
                    @RequestHeader(value = "X-User-Id", required = false) String userId,
                    @RequestBody(required = false) WorkflowValidationRequestDto request);
}
