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
    private final com.qualiapproche.support.service.DemandeDocumentService demandeService;

    @PostMapping("/documents/{documentId}/status")
    public ResponseEntity<Void> updateDocumentStatus(
            @PathVariable("documentId") UUID documentId,
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received workflow callback for document {}: {}", documentId, payload);

        // Contrat unifié émis par workflow-service :
        // { "status": "EN_COURS|APPROVED|REJECTED", "statusName": "<étape atteinte>",
        //   "etatCode": "<état métier>", "comments": "...", "decision": "...", "timestamp": "..." }
        // `status` est la valeur machine qui pilote le cycle de vie du document ; `statusName` n'est
        // que le libellé de l'étape courante et ne doit plus servir de test (les libellés réels
        // — « Validation RS », « Suivi RQ »… — ne correspondaient à aucun des cas attendus).
        String status = (String) payload.get("status");
        String statusName = (String) payload.get("statusName");
        String comments = (String) payload.get("comments");

        documentService.updateWorkflowStatus(documentId, status, statusName, comments);

        return ResponseEntity.ok().build();
    }

    /**
     * Avancement d'une demande de modification ou de suppression.
     *
     * <p>Point de remise distinct de celui des documents : la décision finale sur une demande
     * n'est pas qu'un état à consigner — une suppression acceptée retire le document, une
     * modification acceptée ouvre le dépôt du fichier remplaçant.</p>
     */
    @PostMapping("/demandes/{demandeId}/status")
    public ResponseEntity<Void> updateDemandeStatus(
            @PathVariable("demandeId") UUID demandeId,
            @RequestBody Map<String, Object> payload) {

        log.info("Received workflow callback for demande {}: {}", demandeId, payload);
        demandeService.traiterAvancement(demandeId, payload);
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
