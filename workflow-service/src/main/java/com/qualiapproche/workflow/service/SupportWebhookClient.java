package com.qualiapproche.workflow.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "support-service")
public interface SupportWebhookClient {

    @PostMapping("/api/v1/internal/callbacks/documents/{documentId}/status")
    void updateDocumentStatus(@PathVariable("documentId") UUID documentId, @RequestBody Map<String, Object> payload);

    @PostMapping("/api/v1/internal/callbacks/documents/{documentId}/audit")
    void logDocumentAudit(@PathVariable("documentId") UUID documentId, @RequestBody Map<String, Object> payload);

    /**
     * Avancement d'une demande de modification ou de suppression.
     *
     * <p>Distinct du point de remise des documents : la décision finale sur une demande ne se borne
     * pas à consigner un état — elle ouvre le dépôt d'un fichier remplaçant, ou retire le document.</p>
     */
    @PostMapping("/api/v1/internal/callbacks/demandes/{demandeId}/status")
    void updateDemandeStatus(@PathVariable("demandeId") UUID demandeId, @RequestBody Map<String, Object> payload);
}
