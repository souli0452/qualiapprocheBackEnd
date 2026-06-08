package com.qualiapproche.support.service;

import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.model.QmsDocumentVersion;
import com.qualiapproche.support.repository.DocumentQmsRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class QmsDocumentService {

    private final DocumentQmsRepository documentRepository;
    private final QmsDocumentVersionRepository versionRepository;
    private final QmsAuditLogService auditLogService;
    private final MinioService minioService;
    private final QmsDocumentTypeService typeService;
    private final WorkflowService workflowService;

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
            String organismeEmetteur,
            String referenceOfficielle,
            String domaine,
            String statutLegal,
            UUID workflowId
    ) {
        log.info("Creating new QMS document: type={}, serviceId={}", documentType, serviceId);

        // 1. Resolve customizable Document Type config
        QmsDocumentType docType = typeService.getTypeByCode(documentType);

        // 2. Auto-generate document number
        String documentNumber = generateDocumentNumber(docType.getCode(), serviceSigle != null ? serviceSigle : "GEN");
        log.info("Generated document number: {}", documentNumber);

        // 3. Upload file to Minio
        String objectName;
        try {
            objectName = minioService.uploadFile(file);
        } catch (Exception e) {
            log.error("Failed to upload file to Minio", e);
            throw new RuntimeException("Échec du dépôt du fichier dans le stockage Minio.");
        }

        // 4. Save metadata to local DB
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
                .status("brouillon")
                .versionMajeure(1)
                .versionMineure(0)
                .periodiciteMois(periodiciteMois != null ? periodiciteMois : 12)
                .confidentiel(confidentiel)
                .documentExterne(documentExterne)
                .organismeEmetteur(organismeEmetteur)
                .referenceOfficielle(referenceOfficielle)
                .datePublication(documentExterne ? LocalDateTime.now() : null)
                .domaine(domaine)
                .statutLegal(statutLegal)
                .archived(false)
                .build();

        document = documentRepository.save(document);

        // 5. Create version history record in local DB
        QmsDocumentVersion version = QmsDocumentVersion.builder()
                .document(document)
                .versionLabel("1.0")
                .dateCreation(LocalDateTime.now())
                .createdBy(getCurrentUser())
                .comment("Dépôt initial")
                .objectName(objectName)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .build();

        versionRepository.save(version);

        // 6. Register Audit log
        auditLogService.logAction("CREATION", documentNumber, "Dépôt initial du document dans Minio");

        // 7. Initiate validation workflow if workflowId is provided
        if (workflowId != null) {
            workflowService.initiateWorkflow(document, workflowId);
            document.setStatus("en_approbation");
            documentRepository.save(document);
        }

        return document;
    }

    @Transactional
    public QmsDocumentVersion addVersion(UUID documentId, MultipartFile file, String comments) throws Exception {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));

        String objectName = minioService.uploadFile(file);

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
                .build();

        version = versionRepository.save(version);

        auditLogService.logAction("MISE_A_JOUR_VERSION", document.getDocumentNumber(), "Nouvelle version ajoutée: " + versionLabel);

        return version;
    }

    /**
     * State transition engine.
     */
    @Transactional
    public DocumentQms transitionStatus(UUID id, String nextStatus, String reason) {
        DocumentQms doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));

        String oldStatus = doc.getStatus();
        log.info("Transitioning document '{}' status: {} -> {}", doc.getDocumentNumber(), oldStatus, nextStatus);

        doc.setStatus(nextStatus);
        doc.setLastModifiedBy(getCurrentUser());
        doc.setLastModifiedReason(reason);

        if ("valide".equals(nextStatus)) {
            doc.setDateVigueur(LocalDateTime.now());
            if (doc.getPeriodiciteMois() != null) {
                doc.setDateProchRevision(doc.getDateVigueur().plusMonths(doc.getPeriodiciteMois()));
            }

            // Incrément version
            doc.setVersionMajeure(doc.getVersionMajeure() + 1);
            doc.setVersionMineure(0);

        }

        doc = documentRepository.save(doc);
        auditLogService.logAction("TRANSITION_STATUT", doc.getDocumentNumber(), "Transition de " + oldStatus + " vers " + nextStatus + ". Raison : " + reason);

        return doc;
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
            // Trigger revision: update status to brouillon
            doc.setStatus("brouillon");
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

        String contentType = filename.toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/octet-stream";

        if (!isPdf) {
            log.warn("Downloaded file for document '{}' is not a PDF. Skipping watermark and returning original content.", doc.getDocumentNumber());
            auditLogService.logAction("TELECHARGEMENT_DOCUMENT", doc.getDocumentNumber(), "Téléchargement direct du document d'origine");
            return new ExportedDocument(originalPdf, filename, contentType);
        }

        // 3. PDFBox Overlay Watermarking
        String versionLabel = doc.getVersionMajeure() + "." + doc.getVersionMineure();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String watermarkText = String.format("%s - V%s - IMPRIME PAR %s LE %s",
                doc.getStatus().toUpperCase(), versionLabel, getCurrentUser().toUpperCase(), timestamp);

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
                StandardProtectionPolicy spp = new StandardProtectionPolicy("qmssecure", "qmssecure", ap);
                spp.setEncryptionKeyLength(128);
                pdDoc.protect(spp);
                log.info("Confidential document password-protected successfully");
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

    public List<DocumentQms> searchDocuments(
            String query,
            String documentType,
            String serviceId,
            List<String> status,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return documentRepository.findAll((root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String likePattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("documentNumber")), likePattern),
                        cb.like(cb.lower(root.get("titre")), likePattern),
                        cb.like(cb.lower(root.get("redacteur")), likePattern),
                        cb.like(cb.lower(root.get("organismeEmetteur")), likePattern)
                ));
            }

            if (documentType != null && !documentType.isBlank()) {
                predicates.add(cb.equal(root.get("documentType"), documentType));
            }

            if (serviceId != null && !serviceId.isBlank()) {
                predicates.add(cb.equal(root.get("serviceId"), serviceId));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(root.get("status").in(status));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateVigueur"), dateFrom.atStartOfDay()));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateVigueur"), dateTo.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
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

    private String getCurrentUser() {
        String fullName = com.qualiapproche.common.utils.SecurityUtils.getCurrentUserFullName();
        if ("Système".equalsIgnoreCase(fullName)) {
            return "system";
        }
        return fullName;
    }
}
