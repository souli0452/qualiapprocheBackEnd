package com.qualiapproche.support.controller;

import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.model.QmsAuditLog;
import com.qualiapproche.support.model.QmsDocumentVersion;
import com.qualiapproche.support.service.QmsAuditLogService;
import com.qualiapproche.support.service.QmsDocumentService;
import com.qualiapproche.support.service.AlfrescoDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_URL;

@Slf4j
@RestController
@RequestMapping(DOCUMENT_URL)
@RequiredArgsConstructor
public class QmsDocumentController {

    private final QmsDocumentService documentService;
    private final QmsAuditLogService auditLogService;
    private final AlfrescoDocumentService alfrescoDocumentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentQms> createDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam("serviceId") String serviceId,
            @RequestParam("serviceLibelle") String serviceLibelle,
            @RequestParam("serviceSigle") String serviceSigle,
            @RequestParam("redacteur") String redacteur,
            @RequestParam(value = "periodiciteMois", required = false) Integer periodiciteMois,
            @RequestParam(value = "confidentiel", required = false, defaultValue = "false") boolean confidentiel,
            @RequestParam(value = "documentExterne", required = false, defaultValue = "false") boolean documentExterne,
            @RequestParam(value = "organismeEmetteur", required = false) String organismeEmetteur,
            @RequestParam(value = "referenceOfficielle", required = false) String referenceOfficielle,
            @RequestParam(value = "domaine", required = false) String domaine,
            @RequestParam(value = "statutLegal", required = false) String statutLegal
    ) {
        DocumentQms doc = documentService.createDocument(
                file, documentType, serviceId, serviceLibelle, serviceSigle, redacteur, periodiciteMois,
                confidentiel, documentExterne, organismeEmetteur, referenceOfficielle, domaine, statutLegal
        );
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<DocumentQms> transitionStatus(
            @PathVariable("id") UUID id,
            @RequestParam("nextStatus") String nextStatus,
            @RequestParam("reason") String reason
    ) {
        DocumentQms doc = documentService.transitionStatus(id, nextStatus, reason);
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/{id}/link-nc")
    public ResponseEntity<DocumentQms> linkToNonConformity(
            @PathVariable("id") UUID id,
            @RequestParam("ncRef") String ncRef,
            @RequestParam("actionCorrective") String actionCorrective
    ) {
        DocumentQms doc = documentService.linkToNonConformity(id, ncRef, actionCorrective);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportSecuredPdf(@PathVariable("id") UUID id) throws IOException {
        QmsDocumentService.ExportedDocument exported = documentService.securedExportPdf(id);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exported.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(exported.getContentType()))
                .body(exported.getBytes());
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<QmsDocumentVersion>> getVersionHistory(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getVersionHistory(id));
    }

    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<List<QmsAuditLog>> getAuditLogs(@PathVariable("id") UUID id) {
        DocumentQms doc = documentService.getDocumentById(id);
        return ResponseEntity.ok(auditLogService.getLogsForDocument(doc.getDocumentNumber()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentQms>> searchDocuments(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "serviceId", required = false) String serviceId,
            @RequestParam(value = "status", required = false) List<String> status,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        List<DocumentQms> results = documentService.searchDocuments(query, documentType, serviceId, status, dateFrom, dateTo);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/users")
    public ResponseEntity<Void> createAlfrescoUser(@RequestBody Map<String, String> body) {
        alfrescoDocumentService.createAlfrescoUser(
                body.get("username"),
                body.get("firstName"),
                body.get("lastName"),
                body.get("email"),
                body.get("password")
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAlfrescoUsers() {
        return ResponseEntity.ok(alfrescoDocumentService.getAlfrescoUsers());
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<Void> setDocumentPermissions(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body
    ) {
        DocumentQms doc = documentService.getDocumentById(id);
        alfrescoDocumentService.setNodePermissions(
                doc.getAlfrescoNodeId(),
                body.get("username"),
                body.get("role"),
                doc.getDocumentNumber()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/share-link")
    public ResponseEntity<Map<String, String>> getShareLink(@PathVariable("id") UUID id) {
        DocumentQms doc = documentService.getDocumentById(id);
        String sharedId = alfrescoDocumentService.getOrCreateShareLink(doc.getAlfrescoNodeId());
        return ResponseEntity.ok(Map.of("sharedId", sharedId));
    }

    @GetMapping("/{id}/aos-url")
    public ResponseEntity<Map<String, String>> getAosUrl(@PathVariable("id") UUID id) {
        DocumentQms doc = documentService.getDocumentById(id);
        String aosUrl = alfrescoDocumentService.getAosUrl(doc.getAlfrescoNodeId());
        if (aosUrl == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ce document ne supporte pas l'édition en direct."));
        }
        return ResponseEntity.ok(Map.of("aosUrl", aosUrl));
    }
}
