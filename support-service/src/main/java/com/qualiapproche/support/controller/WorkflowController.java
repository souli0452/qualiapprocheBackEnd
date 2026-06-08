package com.qualiapproche.support.controller;

import com.qualiapproche.support.model.DocumentWorkflow;
import com.qualiapproche.support.repository.DocumentWorkflowRepository;
import com.qualiapproche.support.service.WorkflowService;
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
    private final DocumentWorkflowRepository workflowRepository;

    @GetMapping
    public ResponseEntity<List<DocumentWorkflow>> getAllWorkflows() {
        return ResponseEntity.ok(workflowRepository.findAll());
    }

    @PostMapping("/documents/{documentId}/validate")
    public ResponseEntity<Void> validateStep(
            @PathVariable UUID documentId,
            @RequestParam String comments,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        workflowService.validateStep(documentId, userId != null ? userId : "system", comments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/{documentId}/reject")
    public ResponseEntity<Void> rejectStep(
            @PathVariable UUID documentId,
            @RequestParam String comments,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        workflowService.rejectStep(documentId, userId != null ? userId : "system", comments);
        return ResponseEntity.ok().build();
    }
}
