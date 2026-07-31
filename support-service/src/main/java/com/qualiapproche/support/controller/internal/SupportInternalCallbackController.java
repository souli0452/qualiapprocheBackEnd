package com.qualiapproche.support.controller.internal;

import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.service.QmsDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/callbacks")
@RequiredArgsConstructor
public class SupportInternalCallbackController {

    private final QmsDocumentService documentService;

    @PostMapping("/documents/{documentId}/status")
    public ResponseEntity<Void> updateDocumentStatus(
            @PathVariable("documentId") UUID documentId,
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received workflow callback for document {}: {}", documentId, payload);
        
        // Example payload: { "status": "APPROVED", "decision": "APPROVE", "comments": "..." }
        String status = (String) payload.get("status");
        String comments = (String) payload.get("comments");
        
        documentService.updateWorkflowStatus(documentId, status, comments);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/{documentId}/audit")
    public ResponseEntity<Void> logDocumentAudit(
            @PathVariable("documentId") UUID documentId,
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received workflow audit for document {}: {}", documentId, payload);
        String action = (String) payload.get("action");
        String details = (String) payload.get("details");
        
        documentService.logWorkflowAudit(documentId, action != null ? action : "WORKFLOW_ACTION", details);
        
        return ResponseEntity.ok().build();
    }
}
