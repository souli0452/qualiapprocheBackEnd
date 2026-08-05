package com.qualiapproche.support.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.support.dto.DemandeDocumentDto;
import com.qualiapproche.support.model.DemandeDocument;
import com.qualiapproche.support.service.DemandeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Demandes de modification et de suppression d'un document.
 *
 * <p>Déposer une demande relève de la lecture du document, non de son écriture : c'est précisément
 * parce qu'on ne peut pas modifier soi-même qu'on en fait la demande. L'instruction, elle, se joue
 * dans le circuit, où chaque étape exige son rôle.</p>
 */
@RestController
@RequestMapping("/api/v1/demandes-document")
@RequiredArgsConstructor
@RequirePermissions(
        // Déposer une demande n'est pas modifier le document : c'est le geste de qui ne peut pas
        // le faire lui-même. La permission est donc propre aux demandes, et non empruntée à
        // l'écriture documentaire — qui l'aurait réservée à ceux qui n'en ont pas besoin.
        create = {"demande-document-write", "document-write"},
        update = {"demande-document-write", "demande-document-validate", "document-write"},
        read = {"demande-document-read", "demande-document-write", "document-read", "document-write"},
        delete = {"demande-document-validate", "document-write"}
)
public class DemandeDocumentController {

    private final DemandeDocumentService demandeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.canCreate(this)")
    public ResponseEntity<DemandeDocumentDto> creer(
            @RequestParam("documentId") UUID documentId,
            @RequestParam("type") DemandeDocument.TypeDemande type,
            @RequestParam("objectif") String objectif,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "file", required = false) MultipartFile pieceJointe) {
        DemandeDocument demande = demandeService.creer(documentId, type, objectif, description, pieceJointe);
        return ResponseEntity.ok(demandeService.getById(demande.getId()));
    }

    /** Demandes visibles de l'appelant : celles de sa structure, toutes pour le responsable qualité. */
    @GetMapping
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<com.qualiapproche.common.response.ApiResponse<List<DemandeDocumentDto>>> mesDemandes() {
        return ResponseEntity.ok(
                com.qualiapproche.common.response.ApiResponse.success(demandeService.mesDemandes()));
    }

    /** Historique des demandes portées sur un document. */
    @GetMapping("/document/{documentId}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<com.qualiapproche.common.response.ApiResponse<List<DemandeDocumentDto>>> parDocument(
            @PathVariable("documentId") UUID documentId) {
        return ResponseEntity.ok(com.qualiapproche.common.response.ApiResponse.success(
                demandeService.demandesDuDocument(documentId)));
    }

    /** Statistiques des demandes visibles : par état, par nature, et dépôts par mois. */
    @GetMapping("/stats")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<com.qualiapproche.common.response.ApiResponse<java.util.Map<String, Object>>> statistiques(
            @RequestParam(value = "mois", required = false, defaultValue = "12") int mois) {
        return ResponseEntity.ok(
                com.qualiapproche.common.response.ApiResponse.success(demandeService.statistiques(mois)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<DemandeDocumentDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(demandeService.getById(id));
    }

    /**
     * Dépôt du document remplaçant, une fois la demande de modification acceptée.
     *
     * <p>Fait monter la version majeure : le changement a été instruit et décidé.</p>
     */
    @PostMapping(value = "/{id}/remplacant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<DemandeDocumentDto> deposerRemplacant(
            @PathVariable("id") UUID id,
            @RequestPart("file") MultipartFile fichier,
            @RequestParam(value = "commentaire", required = false) String commentaire) {
        return ResponseEntity.ok(demandeService.deposerRemplacant(id, fichier, commentaire));
    }
}
