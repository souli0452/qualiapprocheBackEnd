package com.qualiapproche.support.service;

import com.qualiapproche.storage.StorageService;
import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.support.client.WorkflowClient;
import com.qualiapproche.support.dto.DemandeDocumentDto;
import com.qualiapproche.support.model.DemandeDocument;
import com.qualiapproche.support.model.DemandeDocument.EtatDemande;
import com.qualiapproche.support.model.DemandeDocument.TypeDemande;
import com.qualiapproche.support.model.DocumentQms;
import com.qualiapproche.support.repository.DemandeDocumentRepository;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.qualiapproche.common.dto.WorkflowStateDto;

/**
 * Demandes de modification et de suppression d'un document.
 *
 * <p>Une demande suit un circuit de validation, comme le document : elle en tire ses étapes, ses
 * responsables, sa traçabilité et ses notifications. Ce qui lui est propre est l'aboutissement —
 * une modification acceptée ouvre le dépôt du fichier remplaçant, une suppression acceptée retire
 * le document. La demande, elle, subsiste : c'est la seule trace de ce qui a été décidé.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandeDocumentService {

    private static final String FAMILLE = "DEMANDE_DOCUMENT";

    private final DemandeDocumentRepository demandeRepository;
    private final DocumentQmsRepository documentRepository;
    private final QmsDocumentService documentService;
    private final ProfilUtilisateurService profilUtilisateurService;
    private final StorageService storageService;
    private final QmsAuditLogService auditLogService;
    private final WorkflowClient workflowClient;
    private final EtatsDuCircuitService etatsDuCircuit;

    /**
     * Dépose une demande et ouvre son circuit.
     *
     * <p>La structure et le demandeur sont pris de l'utilisateur connecté et jamais saisis : une
     * demande vaut par qui la porte, et laisser choisir sa propre structure ouvrirait la porte à
     * des demandes déposées au nom d'un service auquel on n'appartient pas.</p>
     *
     * <p>Le document doit être visible du demandeur — le contrôle habituel s'applique : on ne
     * demande pas la suppression d'un document qu'on n'a pas le droit de voir.</p>
     */
    @Transactional
    public DemandeDocument creer(UUID documentId, TypeDemande type, String objectif,
                                 String description, MultipartFile pieceJointe) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));
        if (!documentService.peutVoirLeSuiviInterne(document)) {
            throw new AccessDeniedException(
                    "Vous ne pouvez déposer une demande que sur un document de votre structure.");
        }
        if (objectif == null || objectif.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'objectif de la demande est obligatoire : c'est lui qu'instruira le responsable qualité.");
        }

        ProfilUtilisateurService.Profil profil = profilUtilisateurService.profilCourant();

        DemandeDocument demande = DemandeDocument.builder()
                .documentId(documentId)
                .documentNumber(document.getDocumentNumber())
                .documentTitre(document.getTitre())
                .type(type)
                .etat(EtatDemande.EN_COURS)
                .objectif(objectif)
                .description(description)
                .structureId(profil.structureId() != null ? profil.structureId() : document.getServiceId())
                .structureLibelle(document.getServiceLibelle())
                .demandeurId(SecurityUtils.getCurrentUserId())
                .demandeurNom(SecurityUtils.getCurrentUserFullName())
                .build();

        if (pieceJointe != null && !pieceJointe.isEmpty()) {
            try {
                demande.setPieceJointeObjectName(
                        storageService.uploadFile(pieceJointe, "demandes", type.name().toLowerCase()));
                demande.setPieceJointeNom(pieceJointe.getOriginalFilename());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "La pièce jointe n'a pas pu être enregistrée : " + e.getMessage());
            }
        }

        demande = demandeRepository.save(demande);

        // Le circuit est ouvert après l'enregistrement : c'est l'identifiant de la demande qui sert
        // de ressource au circuit, et il n'existe pas avant.
        UUID circuit = circuitDesDemandes();
        WorkflowInstanceDto instance = workflowClient.initiateWorkflow(demande.getId(), FAMILLE, circuit,
                demande.getDocumentNumber());
        demande.setWorkflowId(circuit);
        if (instance != null && instance.getCurrentStateName() != null) {
            demande.setCurrentEtape(instance.getCurrentStateName());
        }

        auditLogService.logAction("DEMANDE_" + type.name(), document.getDocumentNumber(),
                "Demande déposée par " + demande.getDemandeurNom() + " — objectif : " + objectif);

        return demandeRepository.save(demande);
    }

    /** Circuit actif de la famille des demandes ; sans lui, aucune demande ne peut être instruite. */
    private UUID circuitDesDemandes() {
        WorkflowSummaryDto actif;
        try {
            actif = workflowClient.getActiveWorkflowByType(FAMILLE);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le circuit des demandes est introuvable : le service de circuits ne répond pas.");
        }
        if (actif == null || actif.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucun circuit actif n'est configuré pour les demandes de modification et de "
                            + "suppression. Configurez-en un avant de déposer une demande.");
        }
        return actif.getId();
    }

    /**
     * Avancement du circuit, remonté par workflow-service.
     *
     * <p>La décision finale ne se borne pas à consigner un état : une suppression acceptée est
     * exécutée aussitôt — il n'y a rien à attendre de plus — tandis qu'une modification acceptée
     * reste en attente du fichier remplaçant, que seul un humain peut fournir.</p>
     */
    @Transactional
    public void traiterAvancement(UUID demandeId, Map<String, Object> payload) {
        DemandeDocument demande = demandeRepository.findById(demandeId).orElse(null);
        if (demande == null) {
            log.warn("Avancement reçu pour une demande inconnue : {}", demandeId);
            return;
        }

        String statut = (String) payload.get("status");
        String etape = (String) payload.get("statusName");
        String commentaires = (String) payload.get("comments");

        if (etape != null) {
            demande.setCurrentEtape(etape);
        }

        if ("APPROVED".equalsIgnoreCase(statut)) {
            demande.setEtat(EtatDemande.ACCEPTEE);
            demande.setDateDecision(LocalDateTime.now());
            demande.setMotifDecision(commentaires);
            demandeRepository.save(demande);

            if (demande.getType() == TypeDemande.SUPPRESSION) {
                executerSuppression(demande);
            }
            return;
        }

        if ("REJECTED".equalsIgnoreCase(statut)) {
            demande.setEtat(EtatDemande.REFUSEE);
            demande.setDateDecision(LocalDateTime.now());
            demande.setMotifDecision(commentaires);
        }

        demandeRepository.save(demande);
    }

    /** Retire le document et clôt la demande, qui reste consultable. */
    private void executerSuppression(DemandeDocument demande) {
        try {
            String numero = documentService.supprimerSurDecision(
                    demande.getDocumentId(), demande.getMotifDecision());
            demande.setEtat(EtatDemande.EXECUTEE);
            demande.setDateExecution(LocalDateTime.now());
            demandeRepository.save(demande);
            log.info("Document {} supprimé à l'issue de la demande {}", numero, demande.getId());
        } catch (Exception e) {
            // La demande reste ACCEPTEE : l'exécution pourra être reprise, et l'écart entre décidé
            // et exécuté demeure visible plutôt que d'être masqué.
            log.error("Suppression du document {} impossible malgré une demande acceptée : {}",
                    demande.getDocumentId(), e.getMessage());
        }
    }

    /**
     * Dépose le fichier qui remplace le document, à l'issue d'une demande de modification acceptée.
     *
     * <p>La version majeure est incrémentée : le changement a été instruit et décidé, ce n'est pas
     * une retouche.</p>
     */
    @Transactional
    public DemandeDocumentDto deposerRemplacant(UUID demandeId, MultipartFile fichier, String commentaire) {
        DemandeDocument demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + demandeId));
        // Déposer un remplaçant publie une version sur le document : c'est l'acte le plus lourd de
        // cet écran, et il ne peut appartenir qu'à la structure concernée.
        exigerAccesDemande(demande);

        if (demande.getType() != TypeDemande.MODIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette demande n'est pas une demande de modification.");
        }
        if (demande.getEtat() != EtatDemande.ACCEPTEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le document remplaçant ne peut être déposé qu'une fois la demande acceptée. "
                            + "État actuel : " + demande.getEtat() + ".");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier n'a été fourni.");
        }

        try {
            documentService.addVersion(demande.getDocumentId(), fichier,
                    commentaire != null && !commentaire.isBlank()
                            ? commentaire
                            : "Remplacement à l'issue de la demande de modification : " + demande.getObjectif(),
                    true);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le document remplaçant n'a pas pu être enregistré : " + e.getMessage());
        }

        demande.setEtat(EtatDemande.EXECUTEE);
        demande.setDateExecution(LocalDateTime.now());
        return versDto(demandeRepository.save(demande));
    }

    // ------------------------------------------------------------------ consultation

    /**
     * Demandes visibles : celles de la structure de l'appelant, toutes pour le responsable qualité.
     *
     * <p>Sans structure de rattachement — profil incomplet, ou user-service momentanément
     * injoignable — l'appelant ne voit que ses propres dépôts. Une résolution en défaut restreint
     * la vue, elle ne l'élargit jamais.</p>
     */
    public List<DemandeDocumentDto> mesDemandes() {
        ProfilUtilisateurService.Profil profil = profilUtilisateurService.profilCourant();
        List<DemandeDocument> demandes = profil.voitToutesLesStructures()
                ? demandeRepository.findAllByOrderByCreatedAtDesc()
                : (profil.structureId() != null
                        ? demandeRepository.findByStructureIdOrderByCreatedAtDesc(profil.structureId())
                        : demandeRepository.findByDemandeurIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId()));
        return demandes.stream().map(this::versDto).toList();
    }

    /**
     * Demandes sur lesquelles l'appelant a une décision ouverte, l'état de leur circuit joint.
     *
     * <p>C'est le circuit qui les désigne : lui seul sait quel rôle décide de l'étape courante.
     * {@link #mesDemandes()} montre ce que l'appelant a le droit de <b>voir</b> — sa structure, ou
     * tout pour le responsable qualité ; cette liste-ci montre ce qu'il a à <b>faire</b>, ce qui
     * n'est pas la même chose et ne se déduit pas de l'état de la demande.</p>
     *
     * <p>La portée de visibilité reste appliquée par-dessus : une demande hors de portée n'apparaît
     * pas, même si le moteur la nomme. Les deux règles se cumulent, aucune ne remplace l'autre.</p>
     *
     * <p>L'état du circuit accompagne chaque ligne — sans lui, l'écran annoncerait des décisions à
     * prendre sans pouvoir en offrir aucune — et il est demandé en un seul lot.</p>
     */
    public List<DemandeDocumentDto> aTraiterParLAppelant() {
        List<UUID> aDecider = etatsDuCircuit.ressourcesADecider(FAMILLE);
        if (aDecider.isEmpty()) {
            return List.of();
        }

        List<DemandeDocument> demandes = demandeRepository.findAllById(aDecider).stream()
                .filter(this::estAPortee)
                .sorted(java.util.Comparator.comparing(DemandeDocument::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
        if (demandes.isEmpty()) {
            return List.of();
        }

        Map<UUID, WorkflowStateDto> etats = etatsDuCircuit.pourRessources(
                demandes.stream().map(DemandeDocument::getId).toList());

        return demandes.stream()
                .map(demande -> {
                    DemandeDocumentDto dto = versDto(demande);
                    dto.setWorkflowState(etats.get(demande.getId()));
                    return dto;
                })
                .toList();
    }

    /**
     * Applique à une demande prise isolément la règle qui gouverne la liste.
     *
     * <p>Le filtre de la liste ne protégeait qu'elle : il suffisait de connaître un identifiant
     * pour lire la demande d'une autre structure — ou, plus grave, y déposer un document
     * remplaçant, donc publier une version sur un document qu'on n'a pas le droit de voir.</p>
     *
     * <p>Une demande hors de portée est déclarée introuvable plutôt que refusée : répondre
     * « accès refusé » confirmerait à qui n'y a pas droit qu'une demande porte bien cet
     * identifiant.</p>
     */
    private void exigerAccesDemande(DemandeDocument demande) {
        if (!estAPortee(demande)) {
            throw new IllegalArgumentException("Demande introuvable : " + demande.getId());
        }
    }

    /**
     * La demande est-elle à portée de l'appelant ?
     *
     * <p>Même règle que {@link #exigerAccesDemande}, posée en question plutôt qu'en refus : une
     * liste écarte en silence ce qui n'est pas à portée, là où un accès direct doit répondre.</p>
     */
    private boolean estAPortee(DemandeDocument demande) {
        String utilisateur = SecurityUtils.getCurrentUserId();
        ProfilUtilisateurService.Profil profil = profilUtilisateurService.profilCourant();

        return profil.voitToutesLesStructures()
                || (utilisateur != null && utilisateur.equals(demande.getDemandeurId()))
                || (profil.structureId() != null
                        && profil.structureId().equals(demande.getStructureId()));
    }

    /** Demandes portées sur un document — son historique de demandes. */
    public List<DemandeDocumentDto> demandesDuDocument(UUID documentId) {
        DocumentQms document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + documentId));
        if (!documentService.peutVoirLeSuiviInterne(document)) {
            throw new AccessDeniedException(
                    "Les demandes portées sur ce document relèvent de la structure qui l'a émis.");
        }
        return demandeRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream()
                .map(this::versDto).toList();
    }

    public DemandeDocumentDto getById(UUID demandeId) {
        DemandeDocument demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + demandeId));
        exigerAccesDemande(demande);
        return versDto(demande);
    }

    /**
     * Statistiques des demandes visibles de l'appelant : répartition par état, par nature, et
     * dépôts par mois.
     *
     * <p>Même portée que la liste — sa structure, ou toutes pour le responsable qualité — pour
     * qu'un chiffre affiché corresponde exactement à ce que l'écran permet d'ouvrir. Des totaux
     * plus larges que la liste laisseraient chercher des dossiers introuvables.</p>
     */
    public Map<String, Object> statistiques(int mois) {
        List<DemandeDocumentDto> demandes = mesDemandes();

        Map<String, Long> parEtat = demandes.stream().collect(Collectors.groupingBy(
                d -> d.getEtat() != null ? d.getEtat() : "INCONNU",
                LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> parType = demandes.stream().collect(Collectors.groupingBy(
                d -> d.getType() != null ? d.getType() : "INCONNU",
                LinkedHashMap::new, Collectors.counting()));

        int profondeur = Math.max(1, Math.min(mois, 36));
        LocalDate debut = LocalDate.now().withDayOfMonth(1).minusMonths(profondeur - 1L);
        Map<String, Long> parMois = new LinkedHashMap<>();
        for (int i = 0; i < profondeur; i++) {
            parMois.put(debut.plusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")), 0L);
        }
        LocalDateTime seuil = debut.atStartOfDay();
        demandes.stream()
                .map(DemandeDocumentDto::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .filter(date -> !date.isBefore(seuil))
                .map(date -> date.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .filter(parMois::containsKey)
                .forEach(m -> parMois.merge(m, 1L, Long::sum));

        Map<String, Object> statistiques = new LinkedHashMap<>();
        statistiques.put("total", (long) demandes.size());
        // « En attente » compte ce qui appelle un geste : instruction en cours, ou aboutissement
        // décidé mais non exécuté. C'est le seul chiffre sur lequel on agit.
        statistiques.put("enAttente", demandes.stream()
                .filter(d -> "EN_COURS".equals(d.getEtat()) || "ACCEPTEE".equals(d.getEtat()))
                .count());
        statistiques.put("parEtat", parEtat);
        statistiques.put("parType", parType);
        statistiques.put("parMois", parMois);
        return statistiques;
    }

    private DemandeDocumentDto versDto(DemandeDocument demande) {
        return DemandeDocumentDto.builder()
                .id(demande.getId())
                .documentId(demande.getDocumentId())
                .documentNumber(demande.getDocumentNumber())
                .documentTitre(demande.getDocumentTitre())
                .type(demande.getType() != null ? demande.getType().name() : null)
                .etat(demande.getEtat() != null ? demande.getEtat().name() : null)
                .objectif(demande.getObjectif())
                .description(demande.getDescription())
                .structureId(demande.getStructureId())
                .structureLibelle(demande.getStructureLibelle())
                .demandeurId(demande.getDemandeurId())
                .demandeurNom(demande.getDemandeurNom())
                .pieceJointeNom(demande.getPieceJointeNom())
                .workflowId(demande.getWorkflowId())
                .currentEtape(demande.getCurrentEtape())
                .createdAt(demande.getCreatedAt())
                .dateDecision(demande.getDateDecision())
                .motifDecision(demande.getMotifDecision())
                .dateExecution(demande.getDateExecution())
                // Une modification acceptée attend encore son fichier ; une suppression acceptée
                // s'exécute d'elle-même, sauf incident.
                .enAttenteExecution(demande.getEtat() == EtatDemande.ACCEPTEE)
                .build();
    }
}
