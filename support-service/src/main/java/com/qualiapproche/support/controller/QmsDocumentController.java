package com.qualiapproche.support.controller;

import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.model.QmsAuditLog;
import com.qualiapproche.support.model.QmsDocumentVersion;
import com.qualiapproche.support.service.QmsAuditLogService;
import com.qualiapproche.support.service.QmsDocumentService;
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
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_URL;

@Slf4j
@RestController
@RequestMapping(DOCUMENT_URL)
@RequiredArgsConstructor
public class QmsDocumentController {

    private final QmsDocumentService documentService;
    private final QmsAuditLogService auditLogService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentQms> createDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("titre") String titre,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "reference", required = false) String reference,
            @RequestParam(value = "description", required = false) String description,
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
            @RequestParam(value = "statutLegal", required = false) String statutLegal,
            @RequestParam(value = "workflowId", required = false) UUID workflowId
    ) {
        DocumentQms doc = documentService.createDocument(
                file, titre, documentType, reference, description, serviceId, serviceLibelle, serviceSigle, redacteur, periodiciteMois,
                confidentiel, documentExterne, organismeEmetteur, referenceOfficielle, domaine, statutLegal, workflowId
        );
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<QmsDocumentVersion> addVersion(
            @PathVariable("id") UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam("comments") String comments
    ) throws Exception {
        QmsDocumentVersion version = documentService.addVersion(id, file, comments);
        return ResponseEntity.ok(version);
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

    @GetMapping("/{id}")
    public ResponseEntity<DocumentQms> getDocument(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }
}
