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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_URL;
import com.qualiapproche.common.dto.NiveauConfidentialiteDto;
import com.qualiapproche.common.dto.WorkflowStateDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.support.dto.DocumentUpdateDto;
import com.qualiapproche.support.model.DocumentStructureAccess;
import com.qualiapproche.support.service.EtatsDuCircuitService;
import com.qualiapproche.support.service.NiveauxConfidentialiteService;
import com.qualiapproche.support.service.ProfilUtilisateurService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    private final WorkflowClient workflowClient;
    private final EtatsDuCircuitService etatsDuCircuit;
    private final NiveauxConfidentialiteService niveauxConfidentialiteService;
    private final ProfilUtilisateurService profilUtilisateurService;

    /**
     * Niveaux de confidentialité proposables à l'appelant comme critère de recherche.
     *
     * <p>La liste est celle qu'il a le droit de voir, pas le référentiel entier : un niveau
     * inaccessible ne rendrait aucun document, et sa seule présence dans le filtre trahirait un
     * classement qui ne le regarde pas. La restriction elle-même reste appliquée côté recherche —
     * cet écran ne fait que ne pas proposer l'inutile.</p>
     *
     * <p>Retour explicite en {@code ApiResponse}, comme les autres listes de référentiel :
     * {@code GlobalResponseHandler} pagine d'office toute réponse de type {@code List}. Le
     * sélecteur recevait alors un objet de pagination là où il attend un tableau — et se serait
     * de toute façon arrêté aux dix premiers niveaux.</p>
     */
    @GetMapping("/filtres/niveaux-confidentialite")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<ApiResponse<
            List<NiveauConfidentialiteDto>>> niveauxFiltrables() {
        var profil = profilUtilisateurService.profilCourant();
        return ResponseEntity.ok(ApiResponse.success(
                niveauxConfidentialiteService.visiblesPour(
                        profil.roles(), profil.estAdministrateur())));
    }

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
            // Priorité et niveau de confidentialité : facultatifs, choisis dans les référentiels
            // paramétrables. Le libellé accompagne l'identifiant pour que la liste s'affiche sans
            // interroger le référentiel à chaque ligne.
            @RequestParam(value = "prioriteId", required = false) String prioriteId,
            @RequestParam(value = "prioriteLibelle", required = false) String prioriteLibelle,
            @RequestParam(value = "niveauConfidentialiteId", required = false) String niveauConfidentialiteId,
            @RequestParam(value = "niveauConfidentialiteLibelle", required = false) String niveauConfidentialiteLibelle,
            @RequestParam(value = "domaineId", required = false) String domaineId,
            @RequestParam(value = "workflowId", required = false) UUID workflowId
    ) {
        DocumentQms doc = documentService.createDocument(
                file, titre, documentType, reference, description, serviceId, serviceLibelle, serviceSigle, redacteur, periodiciteMois,
                confidentiel, documentExterne, processusDestId, processusDestLibelle, referenceOfficielle, domaine, statutLegal,
                prioriteId, prioriteLibelle, niveauConfidentialiteId, niveauConfidentialiteLibelle,
                domaineId, workflowId
        );
        DocumentQmsDto dto = documentMapper.toDto(doc);
        dto.setAvertissementConfidentialite(documentService.dernierAvertissementDeClassement());
        return ResponseEntity.ok(dto);
    }

    /**
     * Change le niveau de confidentialité d'un document déposé.
     *
     * <p>Un classement se révise : le circuit évolue, un rôle disparaît, un document est classé
     * par erreur. Sans ce point d'entrée, seule une intervention en base y remédiait.</p>
     *
     * <p>Le corps de la réponse porte l'avertissement lorsque le nouveau niveau n'admet aucun
     * des rôles décidant des étapes du circuit — le classement est appliqué malgré tout.</p>
     */
    @PutMapping("/{id}/niveau-confidentialite")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<ApiResponse<String>> reclasser(
            @PathVariable("id") UUID id,
            @RequestParam(value = "niveauConfidentialiteId", required = false) String niveauId,
            @RequestParam(value = "niveauConfidentialiteLibelle", required = false) String niveauLibelle) {
        String avertissement = documentService.reclasser(id, niveauId, niveauLibelle);
        return ResponseEntity.ok(ApiResponse.success(avertissement));
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

    /**
     * Piste d'audit du document. Réservée à sa structure — le destinataire d'un partage consulte
     * et télécharge, il n'a pas à connaître le détail des opérations internes.
     */
    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<QmsAuditLog>> getAuditLogs(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getAuditLogs(id));
    }

    /**
     * Recherche paginée, rendue en {@code Page} explicite.
     *
     * <p>Elle rendait la liste entière : {@code GlobalResponseHandler} la découpait alors en
     * mémoire, à dix éléments faute de consigne, et le tableau n'en recevait jamais davantage.
     * La découpe se fait désormais en base, et le total renvoyé est celui du fonds visible —
     * c'est lui qui permet au tableau de dimensionner sa pagination.</p>
     */
    @GetMapping("/search")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<Page<DocumentQmsDto>> searchDocuments(
            @ParameterObject
            Pageable pageable,
            @RequestParam(value = "query",               required = false) String query,
            @RequestParam(value = "documentType",        required = false) String documentType,
            @RequestParam(value = "serviceId",           required = false) String serviceId,
            @RequestParam(value = "serviceLibelle",      required = false) String serviceLibelle,
            @RequestParam(value = "serviceSigle",        required = false) String serviceSigle,
            @RequestParam(value = "redacteur",           required = false) String redacteur,
            @RequestParam(value = "processusDestId",      required = false) String processusDestId,
            @RequestParam(value = "processusDestLibelle", required = false) String processusDestLibelle,
            @RequestParam(value = "domaine",             required = false) String domaine,
            @RequestParam(value = "domaineId",           required = false) String domaineId,
            @RequestParam(value = "prioriteId",          required = false) String prioriteId,
            @RequestParam(value = "niveauConfidentialiteId", required = false) String niveauConfidentialiteId,
            @RequestParam(value = "statutLegal",         required = false) String statutLegal,
            @RequestParam(value = "reference",           required = false) String reference,
            @RequestParam(value = "ncReference",         required = false) String ncReference,
            @RequestParam(value = "createdById",         required = false) String createdById,
            @RequestParam(value = "currentEtape",       required = false) String currentEtape,
            @RequestParam(value = "status",              required = false) List<String> status,
            @RequestParam(value = "confidentiel",        required = false) Boolean confidentiel,
            @RequestParam(value = "documentExterne",     required = false) Boolean documentExterne,
            @RequestParam(value = "archived",            required = false) Boolean archived,
            @RequestParam(value = "dateVigueurFrom",     required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateVigueurFrom,
            @RequestParam(value = "dateVigueurTo",       required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateVigueurTo,
            @RequestParam(value = "dateRevisionFrom",    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRevisionFrom,
            @RequestParam(value = "dateRevisionTo",      required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRevisionTo,
            @RequestParam(value = "datePublicationFrom",
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePublicationFrom,
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
                .domaineId(domaineId)
                .prioriteId(prioriteId)
                .niveauConfidentialiteId(niveauConfidentialiteId)
                .statutLegal(statutLegal)
                .reference(reference)
                .ncReference(ncReference)
                .createdById(createdById)
                .currentEtape(currentEtape)
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

        return ResponseEntity.ok(documentService.searchDocuments(criteria, pageable)
                .map(this::versDtoSitue));
    }

    /**
     * Documents que l'appelant a à traiter, avec les décisions que le circuit lui ouvre.
     *
     * <p>Distinct de la recherche : celle-ci répond à « qu'ai-je à faire », non à « que puis-je
     * consulter ». Les écrans de traitement se fondaient sur le statut du document et montraient
     * donc à chacun les dossiers de tous — ouverts, puis refusés par le moteur au moment de
     * décider. C'est le circuit qui désigne les dossiers, puisque c'est lui qui porte
     * l'habilitation de chaque étape.</p>
     *
     * <p>L'état du circuit accompagne chaque ligne, demandé en un seul lot : sans lui la liste
     * annoncerait des décisions à prendre sans en offrir aucune, et un appel par ligne aurait
     * multiplié les allers-retours autant que de dossiers.</p>
     *
     * <p>Rendue en {@code ApiResponse} explicite : {@code GlobalResponseHandler} pagine d'office
     * toute réponse de type {@code List}, et l'écran recevrait un objet de pagination là où il
     * attend un tableau.</p>
     */
    @GetMapping("/a-traiter")
    @PreAuthorize("@perm.canRead(this)")
    @Operation(summary = "Documents sur lesquels l'appelant peut décider")
    public ResponseEntity<ApiResponse<List<DocumentQmsDto>>> aTraiter() {
        List<DocumentQms> documents = documentService.aTraiterParLAppelant();
        if (documents.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        Map<UUID, WorkflowStateDto> etats = etatsDuCircuit.pourRessources(
                documents.stream().map(DocumentQms::getId).toList());

        List<DocumentQmsDto> lignes = documents.stream()
                .map(doc -> {
                    DocumentQmsDto dto = versDtoSitue(doc);
                    dto.setWorkflowState(etats.get(doc.getId()));
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(lignes));
    }

    /**
     * Traduit le document en DTO en y ajoutant ce que l'écran ne peut pas deviner : l'appelant
     * relève-t-il de la structure du document, ou n'y accède-t-il que par un partage ?
     */
    private DocumentQmsDto versDtoSitue(DocumentQms doc) {
        DocumentQmsDto dto = documentMapper.toDto(doc);
        dto.setSuiviInterneAutorise(documentService.peutVoirLeSuiviInterne(doc));
        return dto;
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
     * Dépôts par mois, pour la courbe de la vue d'ensemble. Mois vides compris, à zéro.
     */
    @GetMapping("/stats/mensuel")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatsMensuelles(
            @RequestParam(value = "mois", required = false, defaultValue = "12") int mois) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocumentsParMois(mois)));
    }

    /**
     * Statistique générique : regroupe et compte les documents visibles par l'utilisateur
     * connecté (tous les documents si administrateur/manager) selon la dimension demandée.
     * Ajouter une nouvelle statistique ne nécessite qu'une nouvelle constante dans
     * {@link DocumentStatDimension}, ni contrôleur ni service à modifier.
     *
     * <p>Dimensions disponibles : DOCUMENT_TYPE, STATUT, DOMAINE, SERVICE, STATUT_LEGAL,
     * REDACTEUR, ORGANISME_EMETTEUR, WORKFLOW_STATUS, ANNEE_CREATION, MOIS_CREATION,
     * CONFIDENTIALITE, DOCUMENT_EXTERNE.</p>
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

    @Operation(summary = "Modifier les métadonnées d'un document",
            description = "Met à jour les champs descriptifs. Le fichier évolue, lui, par le dépôt "
                    + "d'une nouvelle version. Les champs omis sont conservés.")
    @PutMapping("/{id}")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentQmsDto> updateDocument(
            @PathVariable("id") UUID id,
            @RequestBody DocumentUpdateDto dto) {
        return ResponseEntity.ok(documentMapper.toDto(documentService.updateDocument(id, dto)));
    }

    @Operation(summary = "Archiver un document",
            description = "Le retire des recherches et statistiques courantes sans le détruire.")
    @PostMapping("/{id}/archive")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentQmsDto> archiveDocument(
            @PathVariable("id") UUID id,
            @RequestParam(value = "motif", required = false) String motif) {
        return ResponseEntity.ok(documentMapper.toDto(documentService.archiveDocument(id, motif)));
    }

    @Operation(summary = "Désarchiver un document")
    @PostMapping("/{id}/unarchive")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentQmsDto> unarchiveDocument(
            @PathVariable("id") UUID id,
            @RequestParam(value = "motif", required = false) String motif) {
        return ResponseEntity.ok(documentMapper.toDto(documentService.unarchiveDocument(id, motif)));
    }

    @Operation(summary = "Supprimer un brouillon",
            description = "Réservé aux documents jamais validés. Un document entré en vigueur "
                    + "doit être archivé afin de rester traçable.")
    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.canDelete(this)")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("id") UUID id,
            @RequestParam(value = "motif", required = false) String motif) {
        documentService.deleteDocument(id, motif);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<DocumentQmsDto> getDocument(@PathVariable("id") UUID id) {
        DocumentQms doc = documentService.getDocumentById(id);
        DocumentQmsDto dto = versDtoSitue(doc);
        try {
            WorkflowStateDto state = workflowClient.getWorkflowState(id);
            dto.setWorkflowState(state);
        } catch (Exception e) {
            log.warn("Could not fetch workflow state for document {}: {}", id, e.getMessage());
        }
        return ResponseEntity.ok(dto);
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
     * Partage le document avec une structure choisie au moment du geste : tous ses membres y
     * accèdent alors en lecture et en téléchargement.
     *
     * <p>C'est le seul geste par lequel un document sort de sa structure d'origine autrement que
     * nommément. Il appartient à la structure émettrice — le service le vérifie.</p>
     */
    @PostMapping("/{id}/share/structure")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DocumentStructureAccess> partagerAvecStructure(
            @PathVariable("id") UUID id,
            @RequestParam("structureId") String structureId,
            @RequestParam(value = "structureLibelle", required = false) String structureLibelle) {
        return ResponseEntity.ok(documentService.partagerAvecStructure(id, structureId, structureLibelle));
    }

    /** Retire le partage consenti à une structure. */
    @DeleteMapping("/{id}/share/structure/{structureId}")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<Void> retirerPartageStructure(
            @PathVariable("id") UUID id,
            @PathVariable("structureId") String structureId) {
        documentService.retirerPartageStructure(id, structureId);
        return ResponseEntity.noContent().build();
    }

    /** Structures avec lesquelles ce document est partagé. */
    @GetMapping("/{id}/share/structure")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<DocumentStructureAccess>> getPartagesStructure(
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getPartagesStructure(id));
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
