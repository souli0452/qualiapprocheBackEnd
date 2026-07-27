package com.qualiapproche.support.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.support.dto.DocumentQmsDto;
import com.qualiapproche.support.dto.DocumentSearchCriteria;
import com.qualiapproche.support.dto.DocumentStatDimension;
import com.qualiapproche.support.dto.DocumentStatsDto;
import com.qualiapproche.support.dto.SharedDocumentDto;
import com.qualiapproche.support.mapper.DocumentMapper;
import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.model.DocumentUserAccess;
import com.qualiapproche.support.model.QmsAuditLog;
import com.qualiapproche.support.model.QmsDocumentVersion;
import com.qualiapproche.support.service.QmsAuditLogService;
import com.qualiapproche.support.service.QmsDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_URL;

@Slf4j
@RestController
@RequestMapping(DOCUMENT_URL)
@RequiredArgsConstructor
@RequirePermissions(
        create = {"document-write"},
        update = {"document-write"},
        read = {"document-read", "document-write"},
        delete = {"document-write"}
)
public class QmsDocumentController {

    private final QmsDocumentService documentService;
    private final QmsAuditLogService auditLogService;
    private final DocumentMapper documentMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.canCreate(this)")
    public ResponseEntity<DocumentQmsDto> createDocument(
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
            @RequestParam(value = "processusDestId", required = false) String processusDestId,
            @RequestParam(value = "processusDestLibelle", required = false) String processusDestLibelle,
            @RequestParam(value = "referenceOfficielle", required = false) String referenceOfficielle,
            @RequestParam(value = "domaine", required = false) String domaine,
            @RequestParam(value = "statutLegal", required = false) String statutLegal,
            @RequestParam(value = "workflowId", required = false) UUID workflowId
    ) {
        DocumentQms doc = documentService.createDocument(
                file, titre, documentType, reference, description, serviceId, serviceLibelle, serviceSigle, redacteur, periodiciteMois,
                confidentiel, documentExterne, processusDestId, processusDestLibelle, referenceOfficielle, domaine, statutLegal, workflowId
        );
        return ResponseEntity.ok(documentMapper.toDto(doc));
    }

    @PostMapping("/{id}/workflow")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentQmsDto> assignWorkflow(
            @PathVariable("id") UUID id,
            @RequestParam("workflowId") UUID workflowId
    ) {
        DocumentQms doc = documentService.assignWorkflow(id, workflowId);
        return ResponseEntity.ok(documentMapper.toDto(doc));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<QmsDocumentVersion> addVersion(
            @PathVariable("id") UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam("comments") String comments
    ) throws Exception {
        QmsDocumentVersion version = documentService.addVersion(id, file, comments);
        return ResponseEntity.ok(version);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentQmsDto> transitionStatus(
            @PathVariable("id") UUID id,
            @RequestParam("nextStatus") String nextStatus,
            @RequestParam("reason") String reason
    ) {
        DocumentQms doc = documentService.transitionStatus(id, nextStatus, reason);
        return ResponseEntity.ok(documentMapper.toDto(doc));
    }

    @PostMapping("/{id}/link-nc")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentQmsDto> linkToNonConformity(
            @PathVariable("id") UUID id,
            @RequestParam("ncRef") String ncRef,
            @RequestParam("actionCorrective") String actionCorrective
    ) {
        DocumentQms doc = documentService.linkToNonConformity(id, ncRef, actionCorrective);
        return ResponseEntity.ok(documentMapper.toDto(doc));
    }

    @GetMapping("/{id}/export-pdf")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<byte[]> exportSecuredPdf(@PathVariable("id") UUID id) throws IOException {
        QmsDocumentService.ExportedDocument exported = documentService.securedExportPdf(id);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exported.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(exported.getContentType()))
                .body(exported.getBytes());
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<QmsDocumentVersion>> getVersionHistory(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getVersionHistory(id));
    }

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<QmsAuditLog>> getAuditLogs(@PathVariable("id") UUID id) {
        DocumentQms doc = documentService.getDocumentById(id);
        return ResponseEntity.ok(auditLogService.getLogsForDocument(doc.getDocumentNumber()));
    }

    @GetMapping("/search")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<DocumentQmsDto>> searchDocuments(
            @RequestParam(value = "query",               required = false) String query,
            @RequestParam(value = "documentType",        required = false) String documentType,
            @RequestParam(value = "serviceId",           required = false) String serviceId,
            @RequestParam(value = "serviceLibelle",      required = false) String serviceLibelle,
            @RequestParam(value = "serviceSigle",        required = false) String serviceSigle,
            @RequestParam(value = "redacteur",           required = false) String redacteur,
            @RequestParam(value = "processusDestId",      required = false) String processusDestId,
            @RequestParam(value = "processusDestLibelle", required = false) String processusDestLibelle,
            @RequestParam(value = "domaine",             required = false) String domaine,
            @RequestParam(value = "statutLegal",         required = false) String statutLegal,
            @RequestParam(value = "reference",           required = false) String reference,
            @RequestParam(value = "ncReference",         required = false) String ncReference,
            @RequestParam(value = "createdById",         required = false) String createdById,
            @RequestParam(value = "workflowStatus",      required = false) String workflowStatus,
            @RequestParam(value = "status",              required = false) List<String> status,
            @RequestParam(value = "confidentiel",        required = false) Boolean confidentiel,
            @RequestParam(value = "documentExterne",     required = false) Boolean documentExterne,
            @RequestParam(value = "archived",            required = false) Boolean archived,
            @RequestParam(value = "dateVigueurFrom",     required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateVigueurFrom,
            @RequestParam(value = "dateVigueurTo",       required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateVigueurTo,
            @RequestParam(value = "dateRevisionFrom",    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRevisionFrom,
            @RequestParam(value = "dateRevisionTo",      required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRevisionTo,
            @RequestParam(value = "datePublicationFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePublicationFrom,
            @RequestParam(value = "datePublicationTo",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePublicationTo,
            @RequestParam(value = "createdAtFrom",       required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtFrom,
            @RequestParam(value = "createdAtTo",         required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtTo,
            @RequestParam(value = "sortBy",              required = false) String sortBy,
            @RequestParam(value = "sortDirection",       required = false) String sortDirection
    ) {
        DocumentSearchCriteria criteria = DocumentSearchCriteria.builder()
                .query(query)
                .documentType(documentType)
                .serviceId(serviceId)
                .serviceLibelle(serviceLibelle)
                .serviceSigle(serviceSigle)
                .redacteur(redacteur)
                .processusDestId(processusDestId)
                .processusDestLibelle(processusDestLibelle)
                .domaine(domaine)
                .statutLegal(statutLegal)
                .reference(reference)
                .ncReference(ncReference)
                .createdById(createdById)
                .workflowStatus(workflowStatus)
                .status(status)
                .confidentiel(confidentiel)
                .documentExterne(documentExterne)
                .archived(archived)
                .dateVigueurFrom(dateVigueurFrom)
                .dateVigueurTo(dateVigueurTo)
                .dateRevisionFrom(dateRevisionFrom)
                .dateRevisionTo(dateRevisionTo)
                .datePublicationFrom(datePublicationFrom)
                .datePublicationTo(datePublicationTo)
                .createdAtFrom(createdAtFrom)
                .createdAtTo(createdAtTo)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        List<DocumentQms> docs = documentService.searchDocuments(criteria);
        List<DocumentQmsDto> dtos = docs.stream()
                .map(documentMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Statistiques globales des documents QMS :
     * - total, répartition par type, par statut, par domaine, par service
     * - compteurs de documents en retard, confidentiels, externes
     */
    @GetMapping("/stats")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<DocumentStatsDto> getDocumentStats() {
        return ResponseEntity.ok(documentService.getDocumentStats());
    }

    /**
     * Statistique générique : regroupe et compte les documents visibles par l'utilisateur
     * connecté (tous les documents si administrateur/manager) selon la dimension demandée.
     * Ajouter une nouvelle statistique ne nécessite qu'une nouvelle constante dans
     * {@link DocumentStatDimension}, ni contrôleur ni service à modifier.
     *
     * Dimensions disponibles : DOCUMENT_TYPE, STATUT, DOMAINE, SERVICE, STATUT_LEGAL,
     * REDACTEUR, ORGANISME_EMETTEUR, WORKFLOW_STATUS, ANNEE_CREATION, MOIS_CREATION,
     * CONFIDENTIALITE, DOCUMENT_EXTERNE.
     */
    @Operation(summary = "Statistique générique par dimension",
            description = "Regroupe et compte les documents visibles par l'utilisateur connecté " +
                    "(tous si administrateur/manager) selon la dimension demandée.")
    @GetMapping("/stats/by/{dimension}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<Map<String, Long>> getStatsByDimension(
            @PathVariable DocumentStatDimension dimension,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(documentService.getDocumentStatsByDimension(dimension, includeArchived));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<DocumentQmsDto> getDocument(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentMapper.toDto(documentService.getDocumentById(id)));
    }

    // -------------------------------------------------------------------------
    // Document Access Management (ACL)
    // -------------------------------------------------------------------------

    /**
     * Grant access (READ_ONLY or WRITE) to a user on a specific document.
     * Only the document creator or an admin can perform this action.
     *
     * Request body (form params):
     *   userId      - Keycloak subject UUID of the target user
     *   userFullName - display name of the user
     *   userEmail   - email of the user
     *   role        - READ_ONLY | WRITE
     */
    @PostMapping("/{id}/access")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentUserAccess> grantAccess(
            @PathVariable("id") UUID id,
            @RequestParam("userId") String userId,
            @RequestParam(value = "userFullName", required = false) String userFullName,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam("role") String role
    ) {
        DocumentUserAccess access = documentService.grantAccess(id, userId, userFullName, userEmail, role);
        return ResponseEntity.ok(access);
    }

    /**
     * Revoke access for a specific user from a document.
     */
    @DeleteMapping("/{id}/access/{userId}")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable("id") UUID id,
            @PathVariable("userId") String userId
    ) {
        documentService.revokeAccess(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all users who have been granted access to a document.
     */
    @GetMapping("/{id}/access")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<DocumentUserAccess>> getDocumentAccess(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getDocumentAccess(id));
    }

    // -------------------------------------------------------------------------
    // Documents partagés avec un utilisateur
    // -------------------------------------------------------------------------

    /**
     * Retourne les documents partagés avec l'utilisateur actuellement connecté,
     * avec son rôle sur chaque document (READ_ONLY ou WRITE).
     * Chaque utilisateur peut appeler cet endpoint pour voir ses propres partages.
     */
    @GetMapping("/shared/me")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<SharedDocumentDto>> getMySharedDocuments() {
        return ResponseEntity.ok(documentService.getMySharedDocuments());
    }

    /**
     * (Admin uniquement) Retourne les documents partagés avec un utilisateur spécifique.
     *
     * @param userId ID Keycloak (subject) de l'utilisateur cible
     */
    @GetMapping("/shared/{userId}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<SharedDocumentDto>> getSharedDocumentsForUser(
            @PathVariable("userId") String userId) {
        return ResponseEntity.ok(documentService.getSharedDocuments(userId));
    }
}
