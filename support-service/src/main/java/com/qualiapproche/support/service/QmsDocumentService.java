package com.qualiapproche.support.service;

import com.qualiapproche.storage.StorageService;
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
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import com.qualiapproche.support.model.DocumentStructureAccess;
import com.qualiapproche.support.model.QmsAuditLog;
import com.qualiapproche.support.repository.DocumentStructureAccessRepository;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
@Service
@RequiredArgsConstructor
public class QmsDocumentService {

    private final DocumentQmsRepository documentRepository;
    private final QmsDocumentVersionRepository versionRepository;
    private final DocumentUserAccessRepository accessRepository;
    private final DocumentStructureAccessRepository structureAccessRepository;
    private final ProfilUtilisateurService profilUtilisateurService;
    private final NiveauxConfidentialiteService niveauxConfidentialiteService;
    private final QmsAuditLogService auditLogService;
    private final StorageService storageService;
    private final QmsDocumentTypeService typeService;
    private final MailService mailService;
    private final WorkflowClient workflowClient;
    private final EtatsDuCircuitService etatsDuCircuit;

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
            String prioriteId,
            String prioriteLibelle,
            String niveauConfidentialiteId,
            String niveauConfidentialiteLibelle,
            String domaineId,
            UUID workflowId
    ) {
        log.info("Creating new QMS document: type={}, serviceId={}", documentType, serviceId);

        // Le booléen « confidentiel » n'est plus saisi : il se déduit du niveau de confidentialité,
        // qui dit la même chose en plus précis — quels rôles ont le droit de voir. Le conserver
        // permet au reste du service (protection du PDF exporté, statistiques) de continuer à
        // fonctionner sans que deux réglages puissent se contredire.
        confidentiel = niveauConfidentialiteId != null && !niveauConfidentialiteId.isBlank();

        // 1. Resolve customizable Document Type config
        QmsDocumentType docType = typeService.getTypeByCode(documentType);

        // 2. Verify and resolve workflow for the document type
        UUID finalWorkflowId = circuitDuDepot(docType, workflowId);

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
                    "Le circuit de validation '" + workflow.get("nom")
                            + "' n'a aucune étape configurée. Veuillez configurer les étapes du workflow avant de créer un document."
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
            objectName = storageService.uploadFile(file, processusFolder, typeFolder);
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
                .numeroVersion(0)
                .periodiciteMois(periodiciteMois != null ? periodiciteMois : 12)
                .confidentiel(confidentiel)
                .documentExterne(documentExterne)
                .processusDestId(processusDestId)
                .processusDestLibelle(processusDestLibelle)
                .referenceOfficielle(referenceOfficielle)
                .datePublication(documentExterne ? LocalDateTime.now() : null)
                .domaine(domaine)
                .statutLegal(statutLegal)
                .prioriteId(prioriteId)
                .prioriteLibelle(prioriteLibelle)
                .niveauConfidentialiteId(niveauConfidentialiteId)
                .niveauConfidentialiteLibelle(niveauConfidentialiteLibelle)
                .domaineId(domaineId)
                .archived(false)
                .build();

        document = documentRepository.save(document);

        inscrireAuteurSurDocumentClasse(document);

        // 6. Create version history record in local DB
        QmsDocumentVersion version = QmsDocumentVersion.builder()
                .document(document)
                .versionLabel("v0")
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
        WorkflowInstanceDto workflowInstance = workflowClient.initiateWorkflow(
                document.getId(), "DOCUMENT", finalWorkflowId, document.getDocumentNumber());
        document.setWorkflowId(finalWorkflowId);
        // Le classement se confronte au circuit une fois celui-ci rattaché : c'est lui qui
        // désigne les rôles décideurs.
        avertissementDuDepot.set(avertissementSurLeClassement(document));
        if (workflowInstance != null && workflowInstance.getCurrentStateName() != null) {
            document.setCurrentEtape(workflowInstance.getCurrentStateName());
        }
        document = documentRepository.save(document);

        return document;
    }

    /**
     * Circuit sur lequel ouvrir le dépôt, dans l'ordre où les configurations font autorité.
     *
     * <ol>
     *   <li>le circuit imposé par l'appelant, s'il y en a un ;</li>
     *   <li><b>le circuit réservé à ce type</b>, puis à défaut le circuit par défaut de la famille
     *       {@code DOCUMENT} : une seule question au moteur, qui détient la règle.</li>
     * </ol>
     *
     * <p>Le type ne désigne plus son circuit de son côté : le lien vit sur le circuit, seul endroit
     * où l'unicité du couple (famille, cible) puisse être tenue. Deux faces d'un même fait auraient
     * fini par se contredire, et aucune des deux n'aurait fait autorité.</p>
     *
     * <p>Le repli n'interroge pas le circuit actif du <i>type</i> documentaire ('PRO', 'ENR'…) mais
     * celui de la <b>famille</b> : {@code resourceType} désigne la famille de ressource, seule que
     * sache router la remise des notifications — {@code WorkflowNotificationService} ne connaît que
     * DOCUMENT, NON_CONFORMITE et PLAN_ACTION. Un circuit enregistré sous un code de type
     * documentaire n'aurait jamais pu notifier personne.</p>
     *
     * <p>Extrait de {@code createDocument} pour être vérifiable seul : c'est la règle que
     * l'administrateur configure, et elle se lisait au milieu de cent trente lignes de dépôt.</p>
     *
     * @throws ResponseStatusException en 400 si aucun circuit ne peut être déterminé
     */
    UUID circuitDuDepot(QmsDocumentType docType, UUID circuitImpose) {
        if (circuitImpose != null) {
            return circuitImpose;
        }

        UUID repli = null;
        try {
            // Une seule question au moteur : le circuit réservé à ce type de document, ou à défaut
            // celui de la famille. La règle vit chez lui, elle n'est pas rejouée ici.
            WorkflowSummaryDto circuit = workflowClient.circuitAOuvrir(
                    "DOCUMENT", docType.getId() != null ? docType.getId().toString() : null);
            if (circuit != null) {
                repli = circuit.getId();
            }
        } catch (Exception e) {
            // Ni circuit réservé, ni circuit par défaut, ou service de circuits injoignable : le
            // refus qui suit le dit en clair, plutôt que de laisser remonter une erreur technique.
            log.warn("Aucun circuit à ouvrir pour le type de document « {} » : {}",
                    docType.getLibelle(), e.getMessage());
        }

        if (repli == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucun circuit de validation (workflow) n'est configuré pour le type de document '"
                            + docType.getLibelle() + "'. Veuillez d'abord configurer le type de document "
                            + "avec un workflow valide."
            );
        }
        return repli;
    }

    @Transactional
    public DocumentQms assignWorkflow(UUID documentId, UUID workflowId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));
        exigerAccesInterne(document);

        if (document.getCurrentEtape() != null && !document.getCurrentEtape().isBlank()) {
            throw new IllegalStateException("Ce document est déjà soumis à un circuit de validation.");
        }

        WorkflowInstanceDto workflowInstance = workflowClient.initiateWorkflow(
                document.getId(), "DOCUMENT", workflowId, document.getDocumentNumber());
        document.setWorkflowId(workflowId);
        if (workflowInstance != null && workflowInstance.getCurrentStateName() != null) {
            document.setCurrentEtape(workflowInstance.getCurrentStateName());
        }

        auditLogService.logAction("ASSIGNATION_WORKFLOW", document.getDocumentNumber(), "Un circuit de validation a été assigné au document.");

        return documentRepository.save(document);
    }

    @Transactional
    public QmsDocumentVersion addVersion(UUID documentId, MultipartFile file, String comments) throws Exception {
        return addVersion(documentId, file, comments, false);
    }

    /**
     * @param revision vrai lorsque le dépôt fait passer le document au rang suivant : v0 → v1,
     *                 v1 → v2. Seul le remplacement à l'issue d'une demande de modification
     *                 acceptée le justifie — le changement a été demandé, instruit et décidé.
     *                 Faux pour tout autre dépôt : une correction pendant le circuit, ou une
     *                 reprise après rejet, versent un fichier au même rang. Le document ne
     *                 change de rang que lorsque sa révision a été validée.
     */
    @Transactional
    public QmsDocumentVersion addVersion(UUID documentId, MultipartFile file, String comments,
                                         boolean revision) throws Exception {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document introuvable"));
        exigerAccesInterne(document);

        // No file extension restriction enforced here to allow any file type (png, pdf, word, etc.) up to 1GB.

        QmsDocumentType docType = typeService.getTypeByCode(document.getDocumentType());
        String processusFolder = (document.getServiceSigle() != null && !document.getServiceSigle().isBlank())
                ? document.getServiceSigle() : document.getServiceLibelle();
        String typeFolder = (docType.getFolderName() != null && !docType.getFolderName().isBlank())
                ? docType.getFolderName() : docType.getCode();
        String objectName = storageService.uploadFile(file, processusFolder, typeFolder);

        // Le document était validé : le contenu change, il doit repasser par le circuit de validation
        // avant de pouvoir de nouveau être considéré comme "VALIDE" (ISO 9001 §7.5.2).
        boolean requiresRevalidation = document.isEsTraiter();

        if (revision) {
            document.setNumeroVersion(document.getNumeroVersion() + 1);
        }
        documentRepository.save(document);

        String versionLabel = "v" + document.getNumeroVersion();

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
                WorkflowInstanceDto newInstance = workflowClient.initiateWorkflow(
                        document.getId(), "DOCUMENT", prevWorkflowId, document.getDocumentNumber());
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
        exigerAccesInterne(doc);

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

            // Le rang ne bouge pas ici : l'entrée en vigueur consacre la version en cours, elle
            // n'en crée pas une nouvelle. Un document déposé puis validé reste en v0 ; c'est le
            // remplacement à l'issue d'une demande de modification qui le fait passer en v1.
            // L'incrémenter des deux côtés faisait sauter un rang à chaque révision.
        } else if ("obsolete".equalsIgnoreCase(nextStatus)) {
            doc.setEsTraiter(false);
            doc.setObsolete(true);
        } else if ("brouillon".equalsIgnoreCase(nextStatus)) {
            doc.setEsTraiter(false);
            doc.setObsolete(false);
            doc.setCurrentEtape(null);
        } else if ("en_approbation".equalsIgnoreCase(nextStatus) || "en_cours".equalsIgnoreCase(nextStatus)) {
            if (doc.getWorkflowId() != null) {
                WorkflowInstanceDto wfInstance = workflowClient.initiateWorkflow(
                        doc.getId(), "DOCUMENT", doc.getWorkflowId(), doc.getDocumentNumber());
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
        auditLogService.logAction("TRANSITION_STATUT", doc.getDocumentNumber(), "Transition de " + oldState + " vers " + nextStatus + ". Raison : "
                + reason);

        return doc;
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
        exigerAccesInterne(doc);

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
                           List<String> modifications, Consumer<String> setter) {
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
        exigerAccesInterne(doc);

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
        exigerAccesInterne(doc);

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
        exigerAccesInterne(doc);

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

    /**
     * Retire un document à l'issue d'une demande de suppression acceptée.
     *
     * <p>Distinct de {@link #deleteDocument(UUID, String)}, qui refuse tout document entré en
     * vigueur : ici la suppression a précisément été instruite et décidée par un circuit, et c'est
     * cette décision qui l'autorise. Le fichier est retiré du stockage ; la demande, son circuit et
     * la piste d'audit demeurent — ce qui a été décidé, par qui et pourquoi ne disparaît pas avec
     * le contenu.</p>
     *
     * <p>Le contrôle d'accès n'est pas rejoué : l'appelant est le traitement d'aboutissement, et
     * l'habilitation a été donnée à l'étape de décision.</p>
     */
    @Transactional
    public String supprimerSurDecision(UUID documentId, String motif) {
        DocumentQms doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        String numero = doc.getDocumentNumber();

        // Les fichiers d'abord : une ligne supprimée sans son contenu laisserait des objets
        // orphelins dans le stockage, que plus rien ne désignerait.
        doc.getVersions().forEach(version -> {
            try {
                if (version.getObjectName() != null) {
                    storageService.deleteFile(version.getObjectName());
                }
            } catch (Exception e) {
                log.error("Fichier {} non supprimé du stockage : {}", version.getObjectName(), e.getMessage());
            }
        });

        documentRepository.delete(doc);
        auditLogService.logAction("SUPPRESSION_SUR_DEMANDE", numero,
                "Document supprimé à l'issue d'une demande instruite."
                        + (motif != null && !motif.isBlank() ? " Motif : " + motif : ""));
        return numero;
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
        exigerAccesInterne(doc);

        log.info("Linking document '{}' with NC '{}'. Action corrective : {}", doc.getDocumentNumber(), ncRef, actionCorrective);

        doc.setNcReference(ncRef);

        if ("mise_a_jour_document".equalsIgnoreCase(actionCorrective)) {
            // Trigger revision: reset step and esTraiter to back to draft
            doc.setEsTraiter(false);
            doc.setObsolete(false);
            // Le document repasse en rédaction sans changer de rang : la révision n'est pas
            // encore faite, seulement décidée. Le rang suivra le remplacement effectif, s'il
            // passe par une demande de modification.
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

        public byte[] getBytes() {
            return bytes;
        }
        public String getFilename() {
            return filename;
        }
        public String getContentType() {
            return contentType;
        }
    }

    /**
     * Export secured PDF overlayed with custom watermark using PDFBox, and password protected if confidential.
     */
    public ExportedDocument securedExportPdf(UUID id) throws IOException {
        DocumentQms doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));
        exigerAcces(doc);

        log.info("Exporting secured PDF for document '{}'", doc.getDocumentNumber());

        List<QmsDocumentVersion> versions = versionRepository.findByDocumentIdOrderByDateCreationDesc(doc.getId());
        if (versions.isEmpty()) {
            throw new RuntimeException("Aucun fichier ou version trouvé pour ce document.");
        }

        QmsDocumentVersion lastVersion = versions.get(0);
        String objectName = lastVersion.getObjectName();
        String filename = lastVersion.getOriginalFilename() != null ? lastVersion.getOriginalFilename() : "document_" + doc.getDocumentNumber()
                + ".pdf";

        byte[] originalPdf;
        try (InputStream is = storageService.downloadFile(objectName)) {
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
            contentType = Files.probeContentType(Paths.get(filename));
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
        String versionLabel = "v" + doc.getNumeroVersion();
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
        exigerAccesInterne(chargerSansControle(id));
        return versionRepository.findByDocumentIdOrderByDateCreationDesc(id);
    }

    /** Piste d'audit, réservée à la structure du document. */
    public List<QmsAuditLog> getAuditLogs(UUID id) {
        DocumentQms document = chargerSansControle(id);
        exigerAccesInterne(document);
        return auditLogService.getLogsForDocument(document.getDocumentNumber());
    }

    private DocumentQms chargerSansControle(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));
    }

    public DocumentQms getDocumentById(UUID id) {
        DocumentQms document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));
        exigerAcces(document);
        return document;
    }

    // =========================================================================
    // Visibilité d'un document pris isolément
    //
    // Le filtre des recherches ne protège que les listes : sans le contrôle ci-dessous, il suffisait
    // de connaître un identifiant pour ouvrir, télécharger ou faire avancer le document d'une autre
    // structure.
    // =========================================================================

    /** Portée de l'accès d'un utilisateur à un document. */
    private enum Portee {
        /** Aucun droit : le document n'existe pas, de son point de vue. */
        AUCUNE,
        /** Partage : consultation et téléchargement, rien de plus. */
        PARTAGE,
        /** Structure émettrice, auteur, ou responsabilité qualité : le dossier lui appartient. */
        INTERNE
    }

    /** Même question, posée depuis l'extérieur du service (composition du filtre de recherche). */
    public boolean voitToutesLesStructures(ProfilUtilisateurService.Profil profil) {
        return voitTout(profil);
    }

    /**
     * Voit-on l'ensemble des documents, toutes structures confondues ?
     *
     * <p>Deux sources, parce que le rôle peut venir de deux endroits : les rôles techniques portés
     * par le jeton Keycloak, et les rôles applicatifs que détient user-service. Un super
     * administrateur n'a pas nécessairement de rôle technique correspondant — s'en tenir au jeton
     * le ramenait au rang d'utilisateur ordinaire, borné à sa propre structure.</p>
     */
    private boolean voitTout(ProfilUtilisateurService.Profil profil) {
        return SecurityUtils.hasRole("ADMIN")
                || SecurityUtils.hasRole("MANAGE")
                || SecurityUtils.hasRole("SUPER_ADMIN")
                || profil.voitToutesLesStructures();
    }

    private Portee porteeSur(DocumentQms document) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Portee.AUCUNE;
        }

        ProfilUtilisateurService.Profil profil = profilUtilisateurService.profilCourant();

        boolean voitTout = voitTout(profil);
        boolean estAuteur = currentUserId.equals(document.getCreatedById());
        boolean partageNominatif = accessRepository
                .findByDocumentIdAndUserId(document.getId(), currentUserId).isPresent();

        // Le niveau de confidentialité s'applique avant tout le reste, et il est opposable au
        // responsable qualité comme aux autres : voir toutes les structures dispense de la
        // barrière de structure, pas du classement.
        //
        // Deux situations le lèvent. Le partage nominatif, parce qu'il désigne la personne et
        // non un périmètre — un partage à une structure entière y reste soumis, c'est ce qui
        // distingue les deux gestes. Et l'administration générale, pour qu'un document classé
        // sur un rôle que plus personne ne détient reste réparable.
        //
        // L'auteur, lui, n'est pas une exception : il se voit inscrire un accès nominatif au
        // dépôt d'un document classé. Son droit se lit alors dans l'écran de partage, et s'y
        // retire — un accès que nul ne peut révoquer n'a pas sa place dans un système qualité.
        if (!partageNominatif && !profil.estAdministrateur()
                && !niveauxConfidentialiteService.peutVoir(
                        document.getNiveauConfidentialiteId(), profil.roles())) {
            return Portee.AUCUNE;
        }

        if (voitTout || estAuteur
                || (profil.structureId() != null && profil.structureId().equals(document.getServiceId()))) {
            return Portee.INTERNE;
        }

        boolean partageStructure = profil.structureId() != null && structureAccessRepository
                .findByDocumentIdAndStructureId(document.getId(), profil.structureId()).isPresent();

        return (partageNominatif || partageStructure) ? Portee.PARTAGE : Portee.AUCUNE;
    }

    /**
     * Inscrit l'auteur parmi les accès nominatifs d'un document classé.
     *
     * <p>Le classement s'oppose à qui n'a pas le rôle admis, auteur compris : sans cette
     * inscription, un rédacteur pourrait perdre de vue le document qu'il vient de déposer, et ne
     * plus le corriger si le circuit le lui retourne.</p>
     *
     * <p>Un accès nominatif ordinaire, et non une exception dans la règle : il se lit dans
     * l'écran de partage, l'audit le trace, et le responsable qualité peut l'y retirer. Un droit
     * que personne ne peut ni voir ni révoquer n'a pas sa place dans un système qualité.</p>
     */
    private void inscrireAuteurSurDocumentClasse(DocumentQms document) {
        String niveau = document.getNiveauConfidentialiteId();
        if (niveau == null || niveau.isBlank() || document.getCreatedById() == null) {
            return;
        }
        if (accessRepository.findByDocumentIdAndUserId(document.getId(), document.getCreatedById())
                .isPresent()) {
            return;
        }
        accessRepository.save(DocumentUserAccess.builder()
                .document(document)
                .userId(document.getCreatedById())
                .userFullName(document.getRedacteur())
                .role("READ_ONLY")
                .build());
        auditLogService.logAction("PARTAGE_AUTEUR", document.getDocumentNumber(),
                "Accès nominatif inscrit pour le rédacteur : le document est classé.");
    }

    /**
     * Avertissement lorsque le classement d'un document ferme son propre circuit.
     *
     * <p>Les étapes désignent leurs décideurs par rôle ; le niveau de confidentialité en admet
     * un autre ensemble. Rien n'oblige les deux à se recouper — et lorsqu'ils ne se recoupent
     * pas, les titulaires des étapes ne voient pas le document, ne sont pas en mesure de décider,
     * et le dossier s'immobilise sans qu'aucun message ne l'explique.</p>
     *
     * <p>Le dépôt n'est pas refusé : le circuit peut être remanié, ou le niveau changé, et un
     * refus interdirait de classer un document avant d'avoir réglé son circuit. C'est un
     * avertissement, rendu à l'appelant et consigné.</p>
     *
     * @return le message, ou {@code null} si le classement laisse le circuit praticable
     */
    private String avertissementSurLeClassement(DocumentQms document) {
        java.util.Set<String> admis = niveauxConfidentialiteService.rolesAdmis(
                document.getNiveauConfidentialiteId());
        if (admis.isEmpty() || document.getWorkflowId() == null) {
            return null;
        }

        java.util.Set<String> rolesDuCircuit = rolesDecideursDuCircuit(document.getWorkflowId());
        if (rolesDuCircuit.isEmpty()) {
            return null;
        }

        List<String> exclus = rolesDuCircuit.stream()
                .filter(role -> !admis.contains(role))
                .sorted()
                .toList();
        if (exclus.isEmpty()) {
            return null;
        }

        String message = "Le niveau « " + document.getNiveauConfidentialiteLibelle()
                + " » n'admet pas " + String.join(", ", exclus)
                + (exclus.size() > 1 ? ", qui décident" : ", qui décide")
                + " d'étapes du circuit : ces titulaires ne verront pas le document et ne pourront"
                + " pas le traiter. Ajoutez ces rôles au niveau, ou classez le document autrement.";
        log.warn("Document {} : {}", document.getDocumentNumber(), message);
        return message;
    }

    /** Rôles habilités à décider d'au moins une étape du circuit, tels que le circuit les déclare. */
    private java.util.Set<String> rolesDecideursDuCircuit(UUID workflowId) {
        try {
            Map<String, Object> circuit = workflowClient.getWorkflowById(workflowId);
            Object etapes = circuit == null ? null : circuit.get("steps");
            if (!(etapes instanceof List<?> liste)) {
                return java.util.Set.of();
            }
            java.util.Set<String> roles = new java.util.HashSet<>();
            for (Object etape : liste) {
                if (!(etape instanceof Map<?, ?> champs)) {
                    continue;
                }
                ajouterRole(roles, champs.get("responsableRole"));
                if (champs.get("transitions") instanceof List<?> transitions) {
                    for (Object transition : transitions) {
                        if (transition instanceof Map<?, ?> t) {
                            ajouterRole(roles, t.get("requiredRole"));
                        }
                    }
                }
            }
            return roles;
        } catch (Exception e) {
            // Circuit illisible : on ne peut pas se prononcer, et un avertissement inventé serait
            // pire que pas d'avertissement du tout.
            log.warn("Circuit {} illisible, classement non vérifié : {}", workflowId, e.getMessage());
            return java.util.Set.of();
        }
    }

    private void ajouterRole(java.util.Set<String> roles, Object valeur) {
        if (valeur != null && !valeur.toString().isBlank()) {
            roles.add(valeur.toString().trim().toUpperCase());
        }
    }

    /**
     * Change le niveau de confidentialité d'un document déjà déposé.
     *
     * <p>Réservé à l'administration générale et au responsable qualité : le classement décide de
     * qui voit quoi, et un document classé à tort n'est réparable que par eux. Un niveau vide
     * déclasse le document, qui redevient visible de sa structure.</p>
     *
     * <p>L'auteur est réinscrit parmi les accès nominatifs si le document devient classé : sans
     * quoi une reclassification priverait le rédacteur du document qu'il a déposé.</p>
     *
     * @return l'avertissement à afficher si le nouveau classement ferme le circuit, sinon {@code null}
     */
    @Transactional
    public String reclasser(UUID documentId, String niveauId, String niveauLibelle) {
        ProfilUtilisateurService.Profil profil = profilUtilisateurService.profilCourant();
        if (!profil.estAdministrateur() && !profil.estResponsableQualite()) {
            throw new AccessDeniedException(
                    "Le niveau de confidentialité d'un document ne se change qu'au titre de la "
                            + "qualité ou de l'administration générale.");
        }

        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Document introuvable avec l'ID: " + documentId));

        String ancien = document.getNiveauConfidentialiteLibelle();
        boolean classe = niveauId != null && !niveauId.isBlank();
        document.setNiveauConfidentialiteId(classe ? niveauId : null);
        document.setNiveauConfidentialiteLibelle(classe ? niveauLibelle : null);
        document.setConfidentiel(classe);
        document = documentRepository.save(document);

        inscrireAuteurSurDocumentClasse(document);

        auditLogService.logAction("RECLASSEMENT", document.getDocumentNumber(),
                "Niveau de confidentialité : « " + (ancien == null ? "aucun" : ancien)
                        + " » → « " + (classe ? niveauLibelle : "aucun") + " ».");

        return avertissementSurLeClassement(document);
    }

    /**
     * Avertissement du dernier dépôt, relevé par le contrôleur pour le joindre à sa réponse.
     *
     * <p>Porté par la requête et non par le service : deux dépôts simultanés se marcheraient
     * dessus sur un champ partagé.</p>
     */
    private final ThreadLocal<String> avertissementDuDepot = new ThreadLocal<>();

    /** Avertissement du dépôt qui vient d'avoir lieu, consommé une fois. */
    public String dernierAvertissementDeClassement() {
        String message = avertissementDuDepot.get();
        avertissementDuDepot.remove();
        return message;
    }

    private void exigerAcces(DocumentQms document) {
        if (porteeSur(document) == Portee.AUCUNE) {
            // Même message que pour un document inexistant : dire « accès refusé » révélerait à qui
            // n'y a pas droit qu'un document porte bien cet identifiant.
            throw new IllegalArgumentException("Document introuvable avec l'ID: " + document.getId());
        }
    }

    /**
     * Exige mieux qu'un partage : l'historique des versions, la piste d'audit et les décisions du
     * circuit sont les affaires internes de la structure émettrice. Le destinataire d'un partage
     * consulte et télécharge — il n'a pas à savoir qui a rejeté quoi, ni combien de fois.
     */
    private void exigerAccesInterne(DocumentQms document) {
        Portee portee = porteeSur(document);
        if (portee == Portee.AUCUNE) {
            throw new IllegalArgumentException("Document introuvable avec l'ID: " + document.getId());
        }
        if (portee == Portee.PARTAGE) {
            throw new AccessDeniedException(
                    "Ce document vous est partagé en lecture : sa consultation et son téléchargement "
                            + "vous sont ouverts, mais pas son historique ni son suivi interne.");
        }
    }

    /** L'utilisateur peut-il consulter l'historique et le suivi interne de ce document ? */
    public boolean peutVoirLeSuiviInterne(DocumentQms document) {
        return porteeSur(document) == Portee.INTERNE;
    }

    /**
     * Documents sur lesquels l'appelant a une décision ouverte — et eux seuls.
     *
     * <p>C'est le circuit qui les désigne, puisque c'est lui qui porte l'habilitation de chaque
     * étape. Les déduire du statut du document aurait rejoué une seconde règle de visibilité, qui
     * aurait divergé de la première : la vue d'ensemble aurait alors proposé des dossiers que le
     * moteur refuse ensuite de faire avancer.</p>
     *
     * <p>Le classement reste opposable : un document que l'appelant ne peut pas voir n'apparaît pas,
     * même si le circuit le nomme parmi ses décideurs. Le cas n'est pas silencieux pour autant — le
     * dépôt d'un document dont le niveau exclut les rôles du circuit est averti à ce moment-là
     * (voir {@link #avertissementSurLeClassement}).</p>
     */
    @Transactional(readOnly = true)
    public List<DocumentQms> aTraiterParLAppelant() {
        List<UUID> aDecider = etatsDuCircuit.ressourcesADecider("DOCUMENT");
        if (aDecider.isEmpty()) {
            return List.of();
        }
        return documentRepository.findAllById(aDecider).stream()
                .filter(document -> porteeSur(document) != Portee.AUCUNE)
                .sorted(java.util.Comparator.comparing(DocumentQms::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Recherche avancée de documents QMS avec tous les critères disponibles.
     * Les résultats sont automatiquement filtrés par les permissions de l'utilisateur connecté
     * (sauf administrateur / manager qui voit tout).
     */
    public List<DocumentQms> searchDocuments(DocumentSearchCriteria criteria) {
        return documentRepository.findAll(specificationDe(criteria), triDe(criteria));
    }

    /**
     * Même recherche, rendue page par page.
     *
     * <p>La découpe se fait en base et non en mémoire : la variante qui rend une liste chargeait
     * l'intégralité du fonds visible pour n'en présenter qu'une page.</p>
     *
     * @param pageable page demandée ; son tri, s'il est renseigné, prime sur celui des critères
     */
    public Page<DocumentQms> searchDocuments(
            DocumentSearchCriteria criteria, Pageable pageable) {
        Pageable range = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), triDe(criteria));
        return documentRepository.findAll(specificationDe(criteria), range);
    }

    /** Tri retenu pour une recherche : celui demandé, ou du plus récent au plus ancien. */
    private Sort triDe(DocumentSearchCriteria criteria) {
        // Le classement se faisait par numéro de document croissant, si bien qu'un document
        // fraîchement déposé — donc portant le numéro le plus élevé — se retrouvait en toute fin
        // de liste. Son auteur ne le voyait pas revenir de sa création, et le croyait perdu.
        boolean triDemande = criteria.getSortBy() != null && !criteria.getSortBy().isBlank();
        String sortField = triDemande ? criteria.getSortBy() : "createdAt";
        Sort.Direction direction;
        if (criteria.getSortDirection() != null && !criteria.getSortDirection().isBlank()) {
            direction = "DESC".equalsIgnoreCase(criteria.getSortDirection())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
        } else {
            direction = triDemande ? Sort.Direction.ASC : Sort.Direction.DESC;
        }
        return Sort.by(direction, sortField);
    }

    /** Critères de recherche traduits en prédicats, visibilité de l'appelant comprise. */
    private Specification<DocumentQms> specificationDe(
            DocumentSearchCriteria criteria) {
        return (root, cq, cb) -> {
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

            if (criteria.getDomaineId() != null && !criteria.getDomaineId().isBlank()) {
                predicates.add(cb.equal(root.get("domaineId"), criteria.getDomaineId()));
            }

            if (criteria.getPrioriteId() != null && !criteria.getPrioriteId().isBlank()) {
                predicates.add(cb.equal(root.get("prioriteId"), criteria.getPrioriteId()));
            }

            // Le niveau demandé n'élargit rien : addVisibilityPredicates a déjà écarté les
            // documents classés au-dessus des droits de l'appelant, et ce filtre s'y ajoute.
            if (criteria.getNiveauConfidentialiteId() != null
                    && !criteria.getNiveauConfidentialiteId().isBlank()) {
                predicates.add(cb.equal(root.get("niveauConfidentialiteId"),
                        criteria.getNiveauConfidentialiteId()));
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
        };
    }

    /**
     * Restreint toute recherche aux documents que l'appelant a le droit de voir. Partagé par
     * {@link #searchDocuments} et par les méthodes de statistiques, pour qu'elles restent
     * cohérentes entre elles.
     *
     * <p>La règle, telle qu'elle se lit du point de vue de l'utilisateur :</p>
     * <ul>
     *   <li><b>Ma structure</b> — je vois les documents soumis par la structure à laquelle je suis
     *       rattaché. Ce que je peux en faire dépend ensuite de mon rôle, que le circuit arbitre
     *       étape par étape.</li>
     *   <li><b>Les autres structures</b> — je n'en vois rien. Un document ne franchit sa structure
     *       d'origine que si celle-ci le partage explicitement, avec la structure destinataire
     *       entière ({@link DocumentStructureAccess}) ou avec l'un de ses membres
     *       ({@link DocumentUserAccess}).</li>
     *   <li><b>Responsable qualité</b> — voit tout ce qui est soumis, toutes structures
     *       confondues : c'est ce que sa fonction suppose.</li>
     *   <li><b>Mes propres dépôts</b> — visibles quoi qu'il arrive, y compris si mon rattachement
     *       a changé depuis.</li>
     * </ul>
     *
     * <p>La règle précédente ne connaissait ni structure ni responsable qualité : chacun ne voyait
     * que ce qu'il avait lui-même déposé ou ce qu'on lui avait nommément partagé. Deux collègues
     * d'un même service s'ignoraient, et le responsable qualité ne voyait presque rien.</p>
     *
     * <p>Profil indisponible (user-service injoignable) : la structure est nulle, la clause s'y
     * réduit d'elle-même, et l'utilisateur retombe sur ses propres documents et ses partages. Une
     * panne de résolution restreint l'accès, elle ne l'élargit jamais.</p>
     */
    private void addVisibilityPredicates(List<Predicate> predicates, Root<DocumentQms> root,
                                          CriteriaQuery<?> cq, CriteriaBuilder cb) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }

        ProfilUtilisateurService.Profil profil = profilUtilisateurService.profilCourant();

        Join<DocumentQms, DocumentUserAccess> accessJoin = root.join("userAccessList", JoinType.LEFT);
        Join<DocumentQms, DocumentStructureAccess> structureJoin =
                root.join("structureAccessList", JoinType.LEFT);
        cq.distinct(true);

        Predicate estAuteur = cb.equal(root.get("createdById"), currentUserId);
        Predicate partageNominatif = cb.equal(accessJoin.get("userId"), currentUserId);

        // 1. Le classement, opposable au responsable qualité comme aux autres : voir toutes les
        //    structures dispense de la barrière de structure, pas du classement.
        //
        //    Le partage nominatif le lève, parce qu'il désigne la personne et non un périmètre ;
        //    un partage à une structure entière y reste soumis. L'auteur d'un document classé
        //    figure parmi ces partages nominatifs, inscrit au dépôt.
        if (!profil.estAdministrateur()) {
            predicates.add(cb.or(confidentialiteAdmise(root, cb, profil), partageNominatif));
        }

        // 2. La voie d'accès, dont seuls s'affranchissent les rôles qui voient toutes les
        //    structures (super administration, responsable qualité).
        if (voitTout(profil)) {
            return;
        }

        List<Predicate> acces = new ArrayList<>();
        acces.add(estAuteur);
        acces.add(partageNominatif);

        if (profil.structureId() != null) {
            acces.add(cb.equal(root.get("serviceId"), profil.structureId()));
            acces.add(cb.equal(structureJoin.get("structureId"), profil.structureId()));
        }

        predicates.add(cb.or(acces.toArray(new Predicate[0])));
    }

    /**
     * Condition de classement : le niveau du document admet-il l'un des rôles de l'appelant ?
     *
     * <p>Un document sans niveau, ou dont le niveau n'exige aucun rôle, la satisfait toujours.
     * Elle se combine par un OU aux liens qui désignent nommément la personne — dépôt et partage
     * nominatif — et par un ET à tout le reste.</p>
     */
    private Predicate confidentialiteAdmise(Root<DocumentQms> root, CriteriaBuilder cb,
                                            ProfilUtilisateurService.Profil profil) {
        if (niveauxConfidentialiteService.restrictionIndecidable()) {
            // Référentiel injoignable : on ne peut établir le droit de personne sur un document
            // classé. Seuls les documents sans niveau restent admis.
            return cb.isNull(root.get("niveauConfidentialiteId"));
        }

        java.util.Set<String> interdits = niveauxConfidentialiteService.niveauxInterdits(profil.roles());
        if (interdits.isEmpty()) {
            return cb.conjunction();
        }
        return cb.or(
                cb.isNull(root.get("niveauConfidentialiteId")),
                cb.not(root.get("niveauConfidentialiteId").in(interdits)));
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
     * Nombre de documents déposés par mois, sur les {@code mois} derniers mois.
     *
     * <p>Les mois sans dépôt figurent à zéro : une courbe qui saute les mois vides déforme la
     * pente et laisse croire à une activité continue là où il y a eu une interruption.</p>
     *
     * <p>Portée identique au reste des statistiques — ce que l'appelant a le droit de voir, et rien
     * de plus : sa structure, ou l'ensemble s'il accompagne la qualité.</p>
     *
     * @return une carte ordonnée {@code "2026-03" → 4}, du plus ancien au plus récent
     */
    public Map<String, Long> getDocumentsParMois(int mois) {
        int profondeur = Math.max(1, Math.min(mois, 36));
        LocalDate debut = LocalDate.now().withDayOfMonth(1).minusMonths(profondeur - 1L);

        Map<String, Long> serie = new LinkedHashMap<>();
        for (int i = 0; i < profondeur; i++) {
            serie.put(debut.plusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")), 0L);
        }

        LocalDateTime seuil = debut.atStartOfDay();
        visibleDocuments(true).stream()
                .map(DocumentQms::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .filter(date -> !date.isBefore(seuil))
                .map(date -> date.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .filter(serie::containsKey)
                .forEach(mois2 -> serie.merge(mois2, 1L, Long::sum));

        return serie;
    }

    /**
     * Statistiques globales sur les documents. Les administrateurs et responsables qualité les
     * obtiennent sur l'ensemble du fonds ; les autres utilisateurs, sur les seuls documents
     * qu'ils ont déposés ou qui leur ont été partagés.
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

        // Partager relève de la structure émettrice, non du seul déposant : un collègue du même
        // service traite les mêmes dossiers, et le responsable qualité les accompagne tous.
        exigerAccesInterne(document);

        // Le destinataire n'est plus contraint par une structure désignée à la soumission : il se
        // choisit à l'étape où l'on décide de partager, et c'est le geste lui-même — réservé à la
        // structure émettrice — qui fait l'habilitation.

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
     * Partage un document avec une structure, désignée au moment du geste.
     *
     * <p>Le destinataire ne se décide pas à la soumission mais à l'étape où l'on choisit de
     * partager : c'est là qu'on sait à qui le document doit être montré. L'étape en cours est
     * consignée avec le partage — savoir <i>quand</i> un document a franchi sa structure vaut
     * autant que savoir avec qui.</p>
     *
     * <p>Partager relève de la structure émettrice, et du responsable qualité qui accompagne tous
     * les dossiers. L'accès obtenu reste en lecture et téléchargement : ni historique, ni piste
     * d'audit, ni décisions de circuit.</p>
     */
    @Transactional
    public DocumentStructureAccess partagerAvecStructure(
            UUID documentId, String structureId, String structureLibelle) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));
        exigerAccesInterne(document);

        if (structureId == null || structureId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune structure destinataire n'a été désignée pour ce partage.");
        }
        if (structureId.equals(document.getServiceId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cette structure est celle qui a émis le document : ses membres le voient déjà.");
        }

        return structureAccessRepository.findByDocumentIdAndStructureId(documentId, structureId)
                .orElseGet(() -> {
                    var partage = structureAccessRepository.save(
                            DocumentStructureAccess.builder()
                                    .document(document)
                                    .structureId(structureId)
                                    .structureLibelle(structureLibelle)
                                    .etapeCode(document.getCurrentEtape())
                                    .sharedByUserId(SecurityUtils.getCurrentUserId())
                                    .sharedByFullName(getCurrentUser())
                                    .build());

                    auditLogService.logAction("PARTAGE_STRUCTURE", document.getDocumentNumber(),
                            "Document partagé avec la structure "
                                    + (structureLibelle != null ? structureLibelle : structureId)
                                    + (document.getCurrentEtape() != null
                                            ? " à l'étape « " + document.getCurrentEtape() + " »" : ""));
                    return partage;
                });
    }

    /** Retire le partage consenti à une structure. */
    @Transactional
    public void retirerPartageStructure(UUID documentId, String structureId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));
        exigerAccesInterne(document);

        structureAccessRepository.deleteByDocumentIdAndStructureId(documentId, structureId);
        auditLogService.logAction("PARTAGE_STRUCTURE_RETIRE", document.getDocumentNumber(),
                "Partage retiré pour la structure " + structureId);
    }

    /** Structures avec lesquelles le document est partagé. */
    public List<DocumentStructureAccess> getPartagesStructure(UUID documentId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));
        exigerAccesInterne(document);
        return structureAccessRepository.findByDocumentId(documentId);
    }

    /**
     * Revokes access for a specific user from a document.
     */
    @Transactional
    public void revokeAccess(UUID documentId, String userId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));

        // Même règle que pour l'octroi : accorder sans pouvoir révoquer n'aurait pas de sens.
        exigerAccesInterne(document);

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

        // La liste des accès est une affaire interne à la structure émettrice.
        exigerAccesInterne(document);

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

        // Partages nominatifs.
        Map<UUID, SharedDocumentDto> parDocument = new LinkedHashMap<>();
        accessRepository.findByUserId(userId).forEach(access -> {
            DocumentQms doc = access.getDocument();
            parDocument.put(doc.getId(), versSharedDto(doc)
                    .userId(access.getUserId())
                    .userFullName(access.getUserFullName())
                    .userEmail(access.getUserEmail())
                    .accessRole(access.getRole())
                    .partageStructure(false)
                    .build());
        });

        // Partages consentis à la structure entière : ils valent pour chacun de ses membres, sans
        // qu'aucun n'ait été nommé. Sans eux, l'onglet « Partagés avec moi » resterait vide pour
        // qui n'a reçu qu'un partage collectif.
        String structureId = utilisateurCible(userId).structureId();
        if (structureId != null) {
            structureAccessRepository.findByStructureId(structureId).forEach(partage -> {
                DocumentQms doc = partage.getDocument();
                // Un partage nominatif l'emporte : il porte un rôle, que le partage collectif n'a pas.
                parDocument.computeIfAbsent(doc.getId(), id -> versSharedDto(doc)
                        .userId(userId)
                        .accessRole("READ_ONLY")
                        .partageStructure(true)
                        .partagePar(partage.getSharedByFullName())
                        .build());
            });
        }

        return new ArrayList<>(parDocument.values());
    }

    /**
     * Structure de l'utilisateur visé. Pour l'appelant lui-même — le cas courant — c'est son profil
     * en cache ; pour un tiers (consultation par un administrateur), il est demandé à user-service.
     */
    private ProfilUtilisateurService.Profil utilisateurCible(String userId) {
        if (userId.equals(SecurityUtils.getCurrentUserId())) {
            return profilUtilisateurService.profilCourant();
        }
        return profilUtilisateurService.profilDe(userId);
    }

    private SharedDocumentDto.SharedDocumentDtoBuilder versSharedDto(DocumentQms doc) {
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
                .versionLabel("v" + doc.getNumeroVersion())
                .dateVigueur(doc.getDateVigueur())
                .dateProchRevision(doc.getDateProchRevision())
                .confidentiel(doc.isConfidentiel());
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
        if (doc.isArchived()) {
            return "ARCHIVE";
        }
        if (doc.isObsolete()) {
            return "OBSOLETE";
        }
        if (doc.isEsTraiter()) {
            return "VALIDE";
        }
        if (doc.getCurrentEtape() != null && !doc.getCurrentEtape().isBlank()) {
            return doc.getCurrentEtape();
        }
        return "BROUILLON";
    }

    private String getCurrentUser() {
        String fullName = SecurityUtils.getCurrentUserFullName();
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
