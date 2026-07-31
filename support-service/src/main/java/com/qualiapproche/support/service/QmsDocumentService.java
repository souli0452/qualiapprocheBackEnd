package com.qualiapproche.support.service;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.support.dto.DocumentSearchCriteria;
import com.qualiapproche.support.dto.DocumentUpdateDto;
import com.qualiapproche.support.dto.DocumentStatDimension;
import com.qualiapproche.support.dto.DocumentStatsDto;
import com.qualiapproche.support.dto.SharedDocumentDto;
import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.model.DocumentUserAccess;
import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.model.QmsDocumentVersion;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.repository.DocumentUserAccessRepository;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.support.repository.QmsDocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Slf4j
@Service
@RequiredArgsConstructor
public class QmsDocumentService {

    private final DocumentQmsRepository documentRepository;
    private final QmsDocumentVersionRepository versionRepository;
    private final DocumentUserAccessRepository accessRepository;
    private final QmsAuditLogService auditLogService;
    private final MinioService minioService;
    private final QmsDocumentTypeService typeService;
    private final MailService mailService;
    private final WorkflowClient workflowClient;

    /**
     * Creates a new Quality Document (auto-numbering, classification, upload to Minio, persist metadata).
     */
    @Transactional
    public DocumentQms createDocument(
            MultipartFile file,
            String titre,
            String documentType,
            String reference,
            String description,
            String serviceId,
            String serviceLibelle,
            String serviceSigle,
            String redacteur,
            Integer periodiciteMois,
            boolean confidentiel,
            boolean documentExterne,
            String processusDestId,
            String processusDestLibelle,
            String referenceOfficielle,
            String domaine,
            String statutLegal,
            UUID workflowId
    ) {
        log.info("Creating new QMS document: type={}, serviceId={}", documentType, serviceId);

        // 1. Resolve customizable Document Type config
        QmsDocumentType docType = typeService.getTypeByCode(documentType);

        // 2. Verify and resolve workflow for the document type
        UUID finalWorkflowId = workflowId;
        if (finalWorkflowId == null) {
            finalWorkflowId = docType.getWorkflowId();
            
            // Fallback pour compatibilité ascendante si le champ n'est pas encore renseigné en base
            if (finalWorkflowId == null && documentType != null) {
                WorkflowSummaryDto activeWorkflow = workflowClient.getActiveWorkflowByType(documentType);
                if (activeWorkflow != null && activeWorkflow.getId() != null) {
                    finalWorkflowId = activeWorkflow.getId();
                }
            }
        }

        if (finalWorkflowId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun circuit de validation (workflow) n'est configuré pour le type de document '" + docType.getLibelle() + "'. Veuillez d'abord configurer le type de document avec un workflow valide."
            );
        }

        Map<String, Object> workflow = null;
        try {
            workflow = workflowClient.getWorkflowById(finalWorkflowId);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le circuit de validation (workflow) spécifié est introuvable."
            );
        }

        if (workflow == null || workflow.get("steps") == null || ((List<?>) workflow.get("steps")).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le circuit de validation '" + workflow.get("nom") + "' n'a aucune étape configurée. Veuillez configurer les étapes du workflow avant de créer un document."
            );
        }

        // 3. Auto-generate document number
        String documentNumber = generateDocumentNumber(docType.getCode(), serviceSigle != null ? serviceSigle : "GEN");
        log.info("Generated document number: {}", documentNumber);

        // 4. Upload file to Minio, rangé sous {service}/{typeDocument}/
        String objectName;
        try {
            String processusFolder = (serviceSigle != null && !serviceSigle.isBlank()) ? serviceSigle : serviceLibelle;
            String typeFolder = (docType.getFolderName() != null && !docType.getFolderName().isBlank())
                    ? docType.getFolderName() : docType.getCode();
            objectName = minioService.uploadFile(file, processusFolder, typeFolder);
        } catch (Exception e) {
            log.error("Failed to upload file to Minio", e);
            throw new RuntimeException("Échec du dépôt du fichier dans le stockage Minio.");
        }

        // 5. Save metadata to local DB
        DocumentQms document = DocumentQms.builder()
                .documentNumber(documentNumber)
                .titre(titre != null && !titre.isBlank() ? titre : file.getOriginalFilename())
                .documentType(docType.getCode())
                .reference(reference != null && !reference.isBlank() ? reference : "REF-" + System.currentTimeMillis())
                .description(description)
                .serviceId(serviceId)
                .serviceLibelle(serviceLibelle)
                .serviceSigle(serviceSigle)
                .redacteur(redacteur)
                .versionMajeure(1)
                .versionMineure(0)
                .periodiciteMois(periodiciteMois != null ? periodiciteMois : 12)
                .confidentiel(confidentiel)
                .documentExterne(documentExterne)
                .processusDestId(processusDestId)
                .processusDestLibelle(processusDestLibelle)
                .referenceOfficielle(referenceOfficielle)
                .datePublication(documentExterne ? LocalDateTime.now() : null)
                .domaine(domaine)
                .statutLegal(statutLegal)
                .archived(false)
                .build();

        document = documentRepository.save(document);

        // 6. Create version history record in local DB
        QmsDocumentVersion version = QmsDocumentVersion.builder()
                .document(document)
                .versionLabel("1.0")
                .dateCreation(LocalDateTime.now())
                .createdBy(getCurrentUser())
                .comment("Dépôt initial")
                .objectName(objectName)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileHash(calculateChecksum(file))
                .build();

        versionRepository.save(version);

        // 7. Register Audit log
        auditLogService.logAction("CREATION", documentNumber, "Dépôt initial du document dans Minio");

        // 8. Associate workflow immediately
        WorkflowInstanceDto workflowInstance = workflowClient.initiateWorkflow(document.getId(), "DOCUMENT", finalWorkflowId);
        document.setWorkflowId(finalWorkflowId);
        if (workflowInstance != null && workflowInstance.getCurrentStateName() != null) {
            document.setCurrentEtape(workflowInstance.getCurrentStateName());
        }
        document = documentRepository.save(document);

        return document;
    }

    @Transactional
    public DocumentQms assignWorkflow(UUID documentId, UUID workflowId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        if (document.getCurrentEtape() != null && !document.getCurrentEtape().isBlank()) {
            throw new IllegalStateException("Ce document est déjà soumis à un circuit de validation.");
        }

        WorkflowInstanceDto workflowInstance = workflowClient.initiateWorkflow(document.getId(), "DOCUMENT", workflowId);
        document.setWorkflowId(workflowId);
        if (workflowInstance != null && workflowInstance.getCurrentStateName() != null) {
            document.setCurrentEtape(workflowInstance.getCurrentStateName());
        }
        
        auditLogService.logAction("ASSIGNATION_WORKFLOW", document.getDocumentNumber(), "Un circuit de validation a été assigné au document.");
        
        return documentRepository.save(document);
    }

    @Transactional
    public QmsDocumentVersion addVersion(UUID documentId, MultipartFile file, String comments) throws Exception {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));

        // No file extension restriction enforced here to allow any file type (png, pdf, word, etc.) up to 1GB.

        QmsDocumentType docType = typeService.getTypeByCode(document.getDocumentType());
        String processusFolder = (document.getServiceSigle() != null && !document.getServiceSigle().isBlank())
                ? document.getServiceSigle() : document.getServiceLibelle();
        String typeFolder = (docType.getFolderName() != null && !docType.getFolderName().isBlank())
                ? docType.getFolderName() : docType.getCode();
        String objectName = minioService.uploadFile(file, processusFolder, typeFolder);

        // Le document était validé : le contenu change, il doit repasser par le circuit de validation
        // avant de pouvoir de nouveau être considéré comme "VALIDE" (ISO 9001 §7.5.2).
        boolean requiresRevalidation = document.isEsTraiter();

        // Nouvelle version mineure par défaut
        document.setVersionMineure(document.getVersionMineure() + 1);
        documentRepository.save(document);

        String versionLabel = document.getVersionMajeure() + "." + document.getVersionMineure();

        QmsDocumentVersion version = QmsDocumentVersion.builder()
                .document(document)
                .versionLabel(versionLabel)
                .dateCreation(LocalDateTime.now())
                .createdBy(getCurrentUser())
                .comment(comments)
                .objectName(objectName)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileHash(calculateChecksum(file))
                .build();

        version = versionRepository.save(version);

        auditLogService.logAction("MISE_A_JOUR_VERSION", document.getDocumentNumber(), "Nouvelle version ajoutée: " + versionLabel);

        if (requiresRevalidation) {
            WorkflowInstanceDto previousInstance = null;
            try {
                previousInstance = workflowClient.getLastValidationInstance(documentId);
            } catch (Exception ignored) {
            }

            if (previousInstance != null && previousInstance.getWorkflowId() != null) {
                UUID prevWorkflowId = previousInstance.getWorkflowId();
                WorkflowInstanceDto newInstance = workflowClient.initiateWorkflow(document.getId(), "DOCUMENT", prevWorkflowId);
                document.setWorkflowId(prevWorkflowId);
                if (newInstance != null && newInstance.getCurrentStateName() != null) {
                    document.setCurrentEtape(newInstance.getCurrentStateName());
                }
                documentRepository.save(document);
                auditLogService.logAction("REVALIDATION_REQUISE", document.getDocumentNumber(),
                        "Nouvelle version déposée sur un document validé : le circuit de validation est relancé automatiquement.");
            } else {
                document.setEsTraiter(false);
                document.setCurrentEtape(null);
                documentRepository.save(document);
                auditLogService.logAction("REVALIDATION_REQUISE", document.getDocumentNumber(),
                        "Nouvelle version déposée sur un document validé sans circuit associé : retour en brouillon, " +
                                "une validation doit être assignée manuellement.");
            }
        }

        return version;
    }

    @Transactional
    public DocumentQms transitionStatus(UUID id, String nextStatus, String reason) {
        DocumentQms doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));

        String oldState = getDocumentDisplayState(doc);
        log.info("Transitioning document '{}' state: {} -> {}", doc.getDocumentNumber(), oldState, nextStatus);

        doc.setLastModifiedBy(getCurrentUser());
        doc.setLastModifiedReason(reason);

        if ("valide".equalsIgnoreCase(nextStatus) || "traite".equalsIgnoreCase(nextStatus)) {
            doc.setEsTraiter(true);
            doc.setObsolete(false);
            doc.setEnRetardRevision(false);

            doc.setDateVigueur(LocalDateTime.now());
            if (doc.getPeriodiciteMois() != null) {
                doc.setDateProchRevision(doc.getDateVigueur().plusMonths(doc.getPeriodiciteMois()));
            }

            // Incrément version
            doc.setVersionMajeure(doc.getVersionMajeure() + 1);
            doc.setVersionMineure(0);
        } else if ("obsolete".equalsIgnoreCase(nextStatus)) {
            doc.setEsTraiter(false);
            doc.setObsolete(true);
        } else if ("brouillon".equalsIgnoreCase(nextStatus)) {
            doc.setEsTraiter(false);
            doc.setObsolete(false);
            doc.setCurrentEtape(null);
        } else if ("en_approbation".equalsIgnoreCase(nextStatus) || "en_cours".equalsIgnoreCase(nextStatus)) {
            if (doc.getWorkflowId() != null) {
                WorkflowInstanceDto wfInstance = workflowClient.initiateWorkflow(doc.getId(), "DOCUMENT", doc.getWorkflowId());
                if (wfInstance != null && wfInstance.getCurrentStateName() != null) {
                    doc.setCurrentEtape(wfInstance.getCurrentStateName());
                } else {
                    doc.setCurrentEtape("EN_COURS");
                }
            } else {
                doc.setCurrentEtape("EN_COURS");
            }
        }

        doc = documentRepository.save(doc);
        auditLogService.logAction("TRANSITION_STATUT", doc.getDocumentNumber(), "Transition de " + oldState + " vers " + nextStatus + ". Raison : " + reason);

        return doc;
    }

    @Transactional
    public void updateWorkflowStatus(UUID documentId, String status, String comments) {
        DocumentQms doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));
        
        log.info("Received external workflow status update for document '{}': status={}", doc.getDocumentNumber(), status);
        
        doc.setCurrentEtape(status);
        if ("APPROVED".equalsIgnoreCase(status) || "VALIDE".equalsIgnoreCase(status)) {
            transitionStatus(documentId, "valide", comments != null ? comments : "Validation workflow complétée");
        } else if ("REJECTED".equalsIgnoreCase(status) || "REJETE".equalsIgnoreCase(status)) {
            transitionStatus(documentId, "brouillon", comments != null ? comments : "Validation workflow rejetée");
        } else {
            documentRepository.save(doc);
        }
    }

    @Transactional
    public void logWorkflowAudit(UUID documentId, String action, String details) {
        DocumentQms doc = documentRepository.findById(documentId).orElse(null);
        if (doc != null) {
            auditLogService.logAction(action, doc.getDocumentNumber(), details != null ? details : "Action workflow exécutée");
        } else {
            log.warn("Cannot log workflow audit for non-existent document ID: {}", documentId);
        }
    }

    /**
     * Applique au document l'issue d'une transition de workflow.
     *
     * <p>Le cycle de vie du document est piloté par {@code status}, valeur machine émise par
     * workflow-service ({@code EN_COURS} / {@code APPROVED} / {@code REJECTED}), tandis que
     * {@code statusName} n'est que le libellé de l'étape atteinte, affiché tel quel. La version
     * précédente testait le libellé lui-même contre « APPROVED »/« VALIDE » : comme les étapes
     * réelles s'appellent « Validation RS », « Suivi RQ »…, aucun test ne passait jamais et le
     * document restait indéfiniment en brouillon.</p>
     *
     * @param status     issue machine de la transition
     * @param statusName libellé de l'étape atteinte, à afficher
     * @param comments   commentaire saisi lors de la décision
     */
    @Transactional
    public void updateWorkflowStatus(UUID documentId, String status, String statusName, String comments) {
        DocumentQms doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        log.info("Received external workflow status update for document '{}': status={}, étape={}",
                doc.getDocumentNumber(), status, statusName);

        doc.setCurrentEtape(statusName);
        if ("APPROVED".equalsIgnoreCase(status)) {
            transitionStatus(documentId, "valide", comments != null ? comments : "Validation workflow complétée");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            transitionStatus(documentId, "brouillon", comments != null ? comments : "Validation workflow rejetée");
        } else {
            documentRepository.save(doc);
        }
    }

    /**
     * Met à jour les métadonnées d'un document.
     *
     * <p>Aucun point d'entrée ne le permettait : une erreur de saisie sur le titre, le type, le
     * domaine ou la périodicité de révision était définitive, seul le fichier pouvant évoluer via
     * une nouvelle version. Chaque champ laissé à {@code null} est conservé, et la modification est
     * journalisée dans la piste d'audit du document.</p>
     */
    @Transactional
    public DocumentQms updateDocument(UUID documentId, DocumentUpdateDto dto) {
        DocumentQms doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        if (doc.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce document est archivé : désarchivez-le avant de le modifier.");
        }

        List<String> modifications = new ArrayList<>();
        appliquer(dto.getTitre(), doc.getTitre(), "titre", modifications, doc::setTitre);
        appliquer(dto.getReference(), doc.getReference(), "référence", modifications, doc::setReference);
        appliquer(dto.getDescription(), doc.getDescription(), "description", modifications, doc::setDescription);
        appliquer(dto.getServiceId(), doc.getServiceId(), "service", modifications, doc::setServiceId);
        appliquer(dto.getServiceLibelle(), doc.getServiceLibelle(), "libellé du service", modifications, doc::setServiceLibelle);
        appliquer(dto.getServiceSigle(), doc.getServiceSigle(), "sigle du service", modifications, doc::setServiceSigle);
        appliquer(dto.getRedacteur(), doc.getRedacteur(), "rédacteur", modifications, doc::setRedacteur);
        appliquer(dto.getProcessusDestId(), doc.getProcessusDestId(), "processus destinataire", modifications, doc::setProcessusDestId);
        appliquer(dto.getProcessusDestLibelle(), doc.getProcessusDestLibelle(), "libellé du processus", modifications, doc::setProcessusDestLibelle);
        appliquer(dto.getReferenceOfficielle(), doc.getReferenceOfficielle(), "référence officielle", modifications, doc::setReferenceOfficielle);
        appliquer(dto.getDomaine(), doc.getDomaine(), "domaine", modifications, doc::setDomaine);
        appliquer(dto.getStatutLegal(), doc.getStatutLegal(), "statut légal", modifications, doc::setStatutLegal);

        if (dto.getConfidentiel() != null && dto.getConfidentiel() != doc.isConfidentiel()) {
            doc.setConfidentiel(dto.getConfidentiel());
            modifications.add("confidentialité");
        }
        if (dto.getDocumentExterne() != null && dto.getDocumentExterne() != doc.isDocumentExterne()) {
            doc.setDocumentExterne(dto.getDocumentExterne());
            modifications.add("document externe");
        }
        if (dto.getPeriodiciteMois() != null && !dto.getPeriodiciteMois().equals(doc.getPeriodiciteMois())) {
            doc.setPeriodiciteMois(dto.getPeriodiciteMois());
            modifications.add("périodicité de révision");
            // La prochaine échéance de révision découle de la périodicité : la recalculer évite
            // de laisser une date incohérente avec le rythme qui vient d'être choisi.
            if (doc.getDateVigueur() != null) {
                doc.setDateProchRevision(doc.getDateVigueur().plusMonths(dto.getPeriodiciteMois()));
                doc.setEnRetardRevision(doc.getDateProchRevision().isBefore(LocalDateTime.now()));
            }
        }

        if (modifications.isEmpty()) {
            return doc;
        }

        doc.setLastModifiedBy(getCurrentUser());
        doc.setLastModifiedReason(dto.getMotif());
        doc = documentRepository.save(doc);

        auditLogService.logAction("MODIFICATION_METADONNEES", doc.getDocumentNumber(),
                "Champs modifiés : " + String.join(", ", modifications)
                        + (dto.getMotif() != null ? ". Motif : " + dto.getMotif() : ""));
        return doc;
    }

    /** Applique une valeur si elle est fournie et différente, en notant le champ touché. */
    private void appliquer(String nouvelle, String actuelle, String libelle,
                           List<String> modifications, java.util.function.Consumer<String> setter) {
        if (nouvelle != null && !nouvelle.equals(actuelle)) {
            setter.accept(nouvelle);
            modifications.add(libelle);
        }
    }

    /**
     * Archive un document : il sort des recherches et des statistiques courantes sans être détruit.
     *
     * <p>Le champ {@code archived} était filtrable en recherche et compté dans les statistiques,
     * mais n'était jamais positionné : toute la mécanique construite autour restait inerte.
     * L'archivage est la réponse attendue en gestion documentaire, où un document ayant eu une
     * existence officielle doit rester consultable et traçable.</p>
     */
    @Transactional
    public DocumentQms archiveDocument(UUID documentId, String motif) {
        DocumentQms doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        if (doc.isArchived()) {
            return doc;
        }

        doc.setArchived(true);
        doc.setLastModifiedBy(getCurrentUser());
        doc.setLastModifiedReason(motif);
        doc = documentRepository.save(doc);

        auditLogService.logAction("ARCHIVAGE", doc.getDocumentNumber(),
                motif != null ? "Archivage. Motif : " + motif : "Archivage du document.");
        return doc;
    }

    @Transactional
    public DocumentQms unarchiveDocument(UUID documentId, String motif) {
        DocumentQms doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        if (!doc.isArchived()) {
            return doc;
        }

        doc.setArchived(false);
        doc.setLastModifiedBy(getCurrentUser());
        doc.setLastModifiedReason(motif);
        doc = documentRepository.save(doc);

        auditLogService.logAction("DESARCHIVAGE", doc.getDocumentNumber(),
                motif != null ? "Désarchivage. Motif : " + motif : "Désarchivage du document.");
        return doc;
    }

    /**
     * Supprime définitivement un document, uniquement s'il n'a jamais été validé.
     *
     * <p>Restriction volontaire : un document entré en vigueur a valeur de preuve et doit être
     * archivé, pas effacé. Seul un brouillon — jamais validé, non archivé — peut être supprimé,
     * typiquement une création erronée.</p>
     */
    @Transactional
    public void deleteDocument(UUID documentId, String motif) {
        DocumentQms doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        if (doc.isEsTraiter() || doc.getDateVigueur() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce document a été validé et ne peut pas être supprimé. Archivez-le plutôt : "
                            + "un document entré en vigueur doit rester traçable.");
        }

        String numero = doc.getDocumentNumber();
        documentRepository.delete(doc);
        auditLogService.logAction("SUPPRESSION", numero,
                motif != null ? "Suppression du brouillon. Motif : " + motif : "Suppression du brouillon.");
    }

    @Transactional
    public void logWorkflowAudit(UUID documentId, String action, String details) {
        DocumentQms doc = documentRepository.findById(documentId).orElse(null);
        if (doc != null) {
            auditLogService.logAction(action, doc.getDocumentNumber(), details != null ? details : "Action workflow exécutée");
        } else {
            log.warn("Cannot log workflow audit for non-existent document ID: {}", documentId);
        }
    }

    /**
     * Association Linkage between QMS Document and Non-Conformity from amelioration-service.
     */
    @Transactional
    public DocumentQms linkToNonConformity(UUID id, String ncRef, String actionCorrective) {
        DocumentQms doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));

        log.info("Linking document '{}' with NC '{}'. Action corrective : {}", doc.getDocumentNumber(), ncRef, actionCorrective);

        doc.setNcReference(ncRef);

        if ("mise_a_jour_document".equalsIgnoreCase(actionCorrective)) {
            // Trigger revision: reset step and esTraiter to back to draft
            doc.setEsTraiter(false);
            doc.setObsolete(false);
            doc.setVersionMineure(doc.getVersionMineure() + 1);
            doc.setLastModifiedReason("Modification initiée par Action Corrective suite à NC : " + ncRef);
        }

        doc = documentRepository.save(doc);
        auditLogService.logAction("LINK_NC", doc.getDocumentNumber(), "Liaison avec la NC " + ncRef + ". Action : " + actionCorrective);

        return doc;
    }

    public static class ExportedDocument {
        private final byte[] bytes;
        private final String filename;
        private final String contentType;

        public ExportedDocument(byte[] bytes, String filename, String contentType) {
            this.bytes = bytes;
            this.filename = filename;
            this.contentType = contentType;
        }

        public byte[] getBytes() { return bytes; }
        public String getFilename() { return filename; }
        public String getContentType() { return contentType; }
    }

    /**
     * Export secured PDF overlayed with custom watermark using PDFBox, and password protected if confidential.
     */
    public ExportedDocument securedExportPdf(UUID id) throws IOException {
        DocumentQms doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));

        log.info("Exporting secured PDF for document '{}'", doc.getDocumentNumber());

        List<QmsDocumentVersion> versions = versionRepository.findByDocumentIdOrderByDateCreationDesc(doc.getId());
        if (versions.isEmpty()) {
            throw new RuntimeException("Aucun fichier ou version trouvé pour ce document.");
        }

        QmsDocumentVersion lastVersion = versions.get(0);
        String objectName = lastVersion.getObjectName();
        String filename = lastVersion.getOriginalFilename() != null ? lastVersion.getOriginalFilename() : "document_" + doc.getDocumentNumber() + ".pdf";

        byte[] originalPdf;
        try (InputStream is = minioService.downloadFile(objectName)) {
            originalPdf = is.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download file from Minio for objectName: " + objectName, e);
            throw new RuntimeException("Impossible de lire le fichier depuis le stockage Minio.");
        }

        // 2. Check if the downloaded file is a valid PDF
        boolean isPdf = false;
        if (originalPdf.length >= 4) {
            String header = new String(originalPdf, 0, 4);
            isPdf = "%PDF".equals(header);
        }

        String contentType = null;
        try {
            contentType = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(filename));
        } catch (Exception e) {
            log.warn("Failed to probe content type for filename '{}'", filename, e);
        }
        if (contentType == null) {
            contentType = filename.toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/octet-stream";
        }

        if (!isPdf) {
            log.warn("Downloaded file for document '{}' is not a PDF. Skipping watermark and returning original content.", doc.getDocumentNumber());
            auditLogService.logAction("TELECHARGEMENT_DOCUMENT", doc.getDocumentNumber(), "Téléchargement direct du document d'origine");
            return new ExportedDocument(originalPdf, filename, contentType);
        }

        // 3. PDFBox Overlay Watermarking
        String versionLabel = doc.getVersionMajeure() + "." + doc.getVersionMineure();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String watermarkText = String.format("%s - V%s - IMPRIME PAR %s LE %s",
                getDocumentDisplayState(doc), versionLabel, getCurrentUser().toUpperCase(), timestamp);

        try (PDDocument pdDoc = PDDocument.load(originalPdf)) {
            for (PDPage page : pdDoc.getPages()) {
                float width = page.getMediaBox().getWidth();
                float height = page.getMediaBox().getHeight();

                try (PDPageContentStream contentStream = new PDPageContentStream(
                        pdDoc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                    contentStream.setNonStrokingColor(220, 220, 220); // Light Grey

                    contentStream.beginText();
                    contentStream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45), width / 4, height / 4));
                    contentStream.showText(watermarkText);
                    contentStream.endText();
                }
            }

            // 4. Apply password confidentiality encryption
            if (doc.isConfidentiel()) {
                AccessPermission ap = new AccessPermission();
                ap.setCanPrint(true);
                ap.setCanExtractContent(false);
                StandardProtectionPolicy spp = new StandardProtectionPolicy("qmssecure", "", ap);
                spp.setEncryptionKeyLength(128);
                pdDoc.protect(spp);
                log.info("Confidential document password-protected successfully (no open password prompt)");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pdDoc.save(baos);

            auditLogService.logAction("TELECHARGEMENT_PDF_SECURISE", doc.getDocumentNumber(), "Export filigrané sécurisé du document");

            return new ExportedDocument(baos.toByteArray(), filename, "application/pdf");
        }
    }

    public List<QmsDocumentVersion> getVersionHistory(UUID id) {
        return versionRepository.findByDocumentIdOrderByDateCreationDesc(id);
    }

    public DocumentQms getDocumentById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));
    }

    /**
     * Recherche avancée de documents QMS avec tous les critères disponibles.
     * Les résultats sont automatiquement filtrés par les permissions de l'utilisateur connecté
     * (sauf administrateur / manager qui voit tout).
     */
    public List<DocumentQms> searchDocuments(DocumentSearchCriteria criteria) {
        // Tri dynamique
        String sortField = (criteria.getSortBy() != null && !criteria.getSortBy().isBlank())
                ? criteria.getSortBy() : "documentNumber";
        Sort.Direction direction = "DESC".equalsIgnoreCase(criteria.getSortDirection())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);

        return documentRepository.findAll((root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ---- Contrôle d'accès (skip pour admin) ----
            addVisibilityPredicates(predicates, root, cq, cb);

            // ---- Recherche texte libre ----
            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
                String like = "%" + criteria.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("documentNumber")), like),
                        cb.like(cb.lower(root.get("titre")), like),
                        cb.like(cb.lower(root.get("reference")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("redacteur")), like),
                        cb.like(cb.lower(root.get("processusDestLibelle")), like),
                        cb.like(cb.lower(root.get("referenceOfficielle")), like)
                ));
            }

            // ---- Filtres exacts ----
            if (criteria.getDocumentType() != null && !criteria.getDocumentType().isBlank()) {
                predicates.add(cb.equal(root.get("documentType"), criteria.getDocumentType()));
            }

            if (criteria.getServiceId() != null && !criteria.getServiceId().isBlank()) {
                predicates.add(cb.equal(root.get("serviceId"), criteria.getServiceId()));
            }

            if (criteria.getServiceLibelle() != null && !criteria.getServiceLibelle().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("serviceLibelle")),
                        "%" + criteria.getServiceLibelle().toLowerCase() + "%"
                ));
            }

            if (criteria.getServiceSigle() != null && !criteria.getServiceSigle().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("serviceSigle")),
                        criteria.getServiceSigle().toLowerCase()
                ));
            }

            if (criteria.getRedacteur() != null && !criteria.getRedacteur().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("redacteur")),
                        "%" + criteria.getRedacteur().toLowerCase() + "%"
                ));
            }

            if (criteria.getProcessusDestId() != null && !criteria.getProcessusDestId().isBlank()) {
                predicates.add(cb.equal(root.get("processusDestId"), criteria.getProcessusDestId()));
            }

            if (criteria.getProcessusDestLibelle() != null && !criteria.getProcessusDestLibelle().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("processusDestLibelle")),
                        "%" + criteria.getProcessusDestLibelle().toLowerCase() + "%"
                ));
            }

            if (criteria.getDomaine() != null && !criteria.getDomaine().isBlank()) {
                predicates.add(cb.equal(root.get("domaine"), criteria.getDomaine()));
            }

            if (criteria.getStatutLegal() != null && !criteria.getStatutLegal().isBlank()) {
                predicates.add(cb.equal(root.get("statutLegal"), criteria.getStatutLegal()));
            }

            if (criteria.getReference() != null && !criteria.getReference().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("reference")),
                        "%" + criteria.getReference().toLowerCase() + "%"
                ));
            }

            if (criteria.getNcReference() != null && !criteria.getNcReference().isBlank()) {
                predicates.add(cb.equal(root.get("ncReference"), criteria.getNcReference()));
            }

            if (criteria.getCreatedById() != null && !criteria.getCreatedById().isBlank()) {
                predicates.add(cb.equal(root.get("createdById"), criteria.getCreatedById()));
            }

            if (criteria.getCurrentEtape() != null && !criteria.getCurrentEtape().isBlank()) {
                predicates.add(cb.equal(root.get("currentEtape"), criteria.getCurrentEtape()));
            }

            // ---- Filtres multi-valeurs ----
            if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
                List<Predicate> statusPredicates = new ArrayList<>();
                for (String statusVal : criteria.getStatus()) {
                    if ("brouillon".equalsIgnoreCase(statusVal)) {
                        statusPredicates.add(cb.and(
                            cb.isFalse(root.get("esTraiter")),
                            cb.isNull(root.get("currentEtape")),
                            cb.isFalse(root.get("obsolete")),
                            cb.isFalse(root.get("archived"))
                        ));
                    } else if ("en_approbation".equalsIgnoreCase(statusVal)) {
                        statusPredicates.add(cb.and(
                            cb.isNotNull(root.get("currentEtape")),
                            cb.isFalse(root.get("esTraiter")),
                            cb.isFalse(root.get("obsolete")),
                            cb.isFalse(root.get("archived"))
                        ));
                    } else if ("valide".equalsIgnoreCase(statusVal)) {
                        statusPredicates.add(cb.and(
                            cb.isTrue(root.get("esTraiter")),
                            cb.isFalse(root.get("obsolete")),
                            cb.isFalse(root.get("archived"))
                        ));
                    } else if ("obsolete".equalsIgnoreCase(statusVal)) {
                        statusPredicates.add(cb.and(
                            cb.isTrue(root.get("obsolete")),
                            cb.isFalse(root.get("archived"))
                        ));
                    }
                }
                if (!statusPredicates.isEmpty()) {
                    predicates.add(cb.or(statusPredicates.toArray(new Predicate[0])));
                }
            } else {
                // Par défaut, exclure les documents obsolètes et archivés pour éviter des erreurs opérationnelles
                predicates.add(cb.isFalse(root.get("obsolete")));
                predicates.add(cb.isFalse(root.get("archived")));
            }

            // ---- Filtres booléens ----
            if (criteria.getConfidentiel() != null) {
                predicates.add(cb.equal(root.get("confidentiel"), criteria.getConfidentiel()));
            }

            if (criteria.getDocumentExterne() != null) {
                predicates.add(cb.equal(root.get("documentExterne"), criteria.getDocumentExterne()));
            }

            if (criteria.getArchived() != null) {
                predicates.add(cb.equal(root.get("archived"), criteria.getArchived()));
            } else {
                // Par défaut, exclure les documents archivés
                predicates.add(cb.equal(root.get("archived"), false));
            }

            // ---- Filtres de dates ----
            if (criteria.getDateVigueurFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("dateVigueur"), criteria.getDateVigueurFrom().atStartOfDay()));
            }
            if (criteria.getDateVigueurTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("dateVigueur"), criteria.getDateVigueurTo().atTime(LocalTime.MAX)));
            }

            if (criteria.getDateRevisionFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("dateProchRevision"), criteria.getDateRevisionFrom().atStartOfDay()));
            }
            if (criteria.getDateRevisionTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("dateProchRevision"), criteria.getDateRevisionTo().atTime(LocalTime.MAX)));
            }

            if (criteria.getDatePublicationFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("datePublication"), criteria.getDatePublicationFrom().atStartOfDay()));
            }
            if (criteria.getDatePublicationTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("datePublication"), criteria.getDatePublicationTo().atTime(LocalTime.MAX)));
            }

            if (criteria.getCreatedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), criteria.getCreatedAtFrom().atStartOfDay()));
            }
            if (criteria.getCreatedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), criteria.getCreatedAtTo().atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, sort);
    }

    /**
     * Ajoute la restriction de visibilité standard : un administrateur/manager voit tout,
     * un utilisateur normal ne voit que les documents qu'il a créés ou auxquels un accès
     * explicite lui a été accordé ({@link DocumentUserAccess}). Partagé par {@link #searchDocuments}
     * et par les méthodes de statistiques pour qu'elles restent cohérentes entre elles.
     */
    private void addVisibilityPredicates(List<Predicate> predicates, Root<DocumentQms> root,
                                          CriteriaQuery<?> cq, CriteriaBuilder cb) {
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGE");
        String currentUserId = SecurityUtils.getCurrentUserId();

        if (!isAdmin && currentUserId != null) {
            Join<DocumentQms, DocumentUserAccess> accessJoin = root.join("userAccessList", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.equal(root.get("createdById"), currentUserId),
                    cb.equal(accessJoin.get("userId"), currentUserId)
            ));
            cq.distinct(true);
        }
    }

    /**
     * Documents visibles par l'utilisateur connecté (tous si administrateur/manager), tels
     * quels — sans aucun autre filtre — pour servir de base commune aux méthodes de statistiques.
     */
    private List<DocumentQms> visibleDocuments(boolean includeArchived) {
        return documentRepository.findAll((root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addVisibilityPredicates(predicates, root, cq, cb);
            if (!includeArchived) {
                predicates.add(cb.isFalse(root.get("archived")));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    private Map<String, Long> groupBy(List<DocumentQms> documents, DocumentStatDimension dimension) {
        return documents.stream().collect(Collectors.groupingBy(
                doc -> {
                    String value = dimension.extract(doc);
                    return (value == null || value.isBlank()) ? "NON_RENSEIGNE" : value;
                },
                LinkedHashMap::new,
                Collectors.counting()
        ));
    }

    /**
     * Méthode générique de statistiques : regroupe et compte les documents visibles par
     * l'utilisateur connecté (tous si administrateur/manager) selon la dimension demandée.
     * Ajouter une nouvelle statistique = ajouter une constante à {@link DocumentStatDimension},
     * sans toucher à cette méthode.
     */
    public Map<String, Long> getDocumentStatsByDimension(DocumentStatDimension dimension, boolean includeArchived) {
        return groupBy(visibleDocuments(includeArchived), dimension);
    }

    /**
     * Retourne les statistiques globales sur les documents.
     * Les admins/managers voient les stats sur tous les documents ; les autres utilisateurs
     * ne voient que les stats sur leurs propres documents (créés ou partagés avec eux).
     */
    public DocumentStatsDto getDocumentStats() {
        List<DocumentQms> documents = visibleDocuments(false);

        return DocumentStatsDto.builder()
                .totalDocuments(documents.size())
                .countByDocumentType(groupBy(documents, DocumentStatDimension.DOCUMENT_TYPE))
                .countByStatus(groupBy(documents, DocumentStatDimension.STATUT))
                .countByDomaine(groupBy(documents, DocumentStatDimension.DOMAINE))
                .countByService(groupBy(documents, DocumentStatDimension.SERVICE))
                .documentsEnRetardRevision(documents.stream().filter(DocumentQms::isEnRetardRevision).count())
                .documentsConfidentiels(documents.stream().filter(DocumentQms::isConfidentiel).count())
                .documentsExternes(documents.stream().filter(DocumentQms::isDocumentExterne).count())
                .build();
    }

    // =========================================================================
    // Document Access Management
    // =========================================================================

    /**
     * Grants access to a specific user on a document with the given role (READ_ONLY or WRITE).
     * Only the document creator or an admin may grant access.
     */
    @Transactional
    public DocumentUserAccess grantAccess(UUID documentId, String userId, String userFullName, String userEmail, String role) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        String currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGE");

        if (!isAdmin && !document.getCreatedById().equals(currentUserId)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à gérer les accès de ce document.");
        }

        // Update if entry already exists, otherwise create
        DocumentUserAccess access = accessRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElse(DocumentUserAccess.builder()
                        .document(document)
                        .userId(userId)
                        .build());

        access.setUserFullName(userFullName);
        access.setUserEmail(userEmail);
        access.setRole(role.toUpperCase());

        access = accessRepository.save(access);

        auditLogService.logAction("ACCES_ACCORDE", document.getDocumentNumber(),
                "Accès " + role + " accordé à l'utilisateur " + userFullName + " (" + userId + ")");

        // ---- Notification par email (asynchrone) ----
        if (userEmail != null && !userEmail.isBlank()) {
            String sharedByName = getCurrentUser();
            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("userFullName",    userFullName != null ? userFullName : userEmail);
            vars.put("documentNumber", document.getDocumentNumber());
            vars.put("documentTitre",  document.getTitre());
            vars.put("documentType",   document.getDocumentType());
            vars.put("serviceLibelle", document.getServiceLibelle() != null ? document.getServiceLibelle() : "");
            vars.put("role",           role.toUpperCase());
            vars.put("sharedByName",   sharedByName);
            vars.put("appUrl",         "https://quali-sira.qualiapproche.com");

            mailService.sendHtmlEmail(
                    userEmail,
                    "Document partagé avec vous : " + document.getDocumentNumber(),
                    "qmsDocumentShare",
                    vars
            );
        }

        return access;
    }

    /**
     * Revokes access for a specific user from a document.
     */
    @Transactional
    public void revokeAccess(UUID documentId, String userId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        String currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGE");

        if (!isAdmin && !document.getCreatedById().equals(currentUserId)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à révoquer les accès de ce document.");
        }

        accessRepository.deleteByDocumentIdAndUserId(documentId, userId);

        auditLogService.logAction("ACCES_REVOQUE", document.getDocumentNumber(),
                "Accès révoqué pour l'utilisateur (" + userId + ")");
    }

    /**
     * Returns the list of users who have explicit access to a document.
     */
    public List<DocumentUserAccess> getDocumentAccess(UUID documentId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        String currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGE");

        // Only creator or admin can see the full ACL
        if (!isAdmin && !document.getCreatedById().equals(currentUserId)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à consulter les accès de ce document.");
        }

        return accessRepository.findByDocumentId(documentId);
    }

    // =========================================================================
    // Documents partagés avec un utilisateur
    // =========================================================================

    /**
     * Retourne la liste des documents partagés avec un utilisateur donné (par son userId Keycloak),
     * accompagnés de son rôle d'accès sur chaque document.
     * Accessible uniquement par l'utilisateur lui-même ou un admin.
     */
    public List<SharedDocumentDto> getSharedDocuments(String userId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGE");

        if (!isAdmin && !userId.equals(currentUserId)) {
            throw new AccessDeniedException(
                    "Vous n'êtes pas autorisé à consulter les documents partagés d'un autre utilisateur.");
        }

        return accessRepository.findByUserId(userId).stream()
                .map(access -> {
                    DocumentQms doc = access.getDocument();
                    String versionLabel = doc.getVersionMajeure() + "." + doc.getVersionMineure();
                    return SharedDocumentDto.builder()
                            .documentId(doc.getId())
                            .documentNumber(doc.getDocumentNumber())
                            .titre(doc.getTitre())
                            .documentType(doc.getDocumentType())
                            .status(getDocumentDisplayState(doc).toLowerCase())
                            .serviceLibelle(doc.getServiceLibelle())
                            .serviceSigle(doc.getServiceSigle())
                            .redacteur(doc.getRedacteur())
                            .domaine(doc.getDomaine())
                            .versionLabel(versionLabel)
                            .dateVigueur(doc.getDateVigueur())
                            .dateProchRevision(doc.getDateProchRevision())
                            .confidentiel(doc.isConfidentiel())
                            .userId(access.getUserId())
                            .userFullName(access.getUserFullName())
                            .userEmail(access.getUserEmail())
                            .accessRole(access.getRole())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Raccourci : retourne les documents partagés avec l'utilisateur actuellement connecté.
     */
    public List<SharedDocumentDto> getMySharedDocuments() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("Utilisateur non authentifié.");
        }
        return getSharedDocuments(currentUserId);
    }

    private String generateDocumentNumber(String typeCode, String serviceSigle) {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = typeCode.toUpperCase() + "-" + serviceSigle.toUpperCase() + "-" + year + "-";

        String maxDocNum = documentRepository.findMaxDocumentNumberByPrefix(prefix);
        int nextSeq = 1;

        if (maxDocNum != null && maxDocNum.startsWith(prefix)) {
            String seqStr = maxDocNum.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse sequence suffix from: {}", maxDocNum);
            }
        }

        return prefix + String.format("%03d", nextSeq);
    }

    public String getDocumentDisplayState(DocumentQms doc) {
        if (doc.isArchived()) return "ARCHIVE";
        if (doc.isObsolete()) return "OBSOLETE";
        if (doc.isEsTraiter()) return "VALIDE";
        if (doc.getCurrentEtape() != null && !doc.getCurrentEtape().isBlank()) return doc.getCurrentEtape();
        return "BROUILLON";
    }

    private String getCurrentUser() {
        String fullName = com.qualiapproche.common.utils.SecurityUtils.getCurrentUserFullName();
        if ("Système".equalsIgnoreCase(fullName)) {
            return "system";
        }
        return fullName;
    }

    private String calculateChecksum(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesCount;
            while ((bytesCount = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesCount);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to calculate SHA-256 checksum for file: {}", file.getOriginalFilename(), e);
            return null;
        }
    }
}
