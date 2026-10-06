package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.AuditDto;
import com.qualiapproche.evaluation.dto.FiltreProgramme;
import com.qualiapproche.evaluation.dto.RedactionRapportDto;
import com.qualiapproche.evaluation.dto.TransmissionDto;
import com.qualiapproche.evaluation.enumeration.NiveauEfficacite;
import com.qualiapproche.evaluation.enumeration.NiveauRisque;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.service.AuditService;
import com.qualiapproche.evaluation.service.TransmissionVersM05;
import com.qualiapproche.evaluation.service.document.DocumentsAuditService;
import com.qualiapproche.evaluation.service.document.RenduDocument;
import com.qualiapproche.evaluation.dto.RapportAuditDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.AUDITS;

/**
 * Le programme d'audit.
 *
 * <p>Planifier demande {@code audit-write} ; valider le programme et clôturer, qui engagent
 * l'organisation, demandent {@code audit-validate}. Démarrer un audit revient à qui le
 * planifie.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(AUDITS)
@RequirePermissions(
        create = {"audit-write"},
        update = {"audit-write"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-validate"},
        delete = {"audit-write"},
        validate = {"audit-validate"}
)
public class AuditController {

    private final AuditService service;
    private final TransmissionVersM05 transmission;
    private final DocumentsAuditService documents;

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<AuditDto>> page(
            @RequestParam(value = "statuts", required = false) List<StatutAudit> statuts,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "typeAuditId", required = false) UUID typeAuditId,
            @RequestParam(value = "domaineId", required = false) UUID domaineId,
            @RequestParam(value = "niveauRisque", required = false) NiveauRisque niveauRisque,
            @RequestParam(value = "responsableId", required = false) UUID responsableId,
            @RequestParam(value = "enRetard", required = false) Boolean enRetard,
            @RequestParam(value = "mesAudits", defaultValue = "false") boolean mesAudits,
            @RequestParam(value = "annee", required = false) Integer annee,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.rechercher(FiltreProgramme.builder()
                .statuts(statuts).recherche(search).typeAuditId(typeAuditId).domaineId(domaineId)
                .niveauRisque(niveauRisque).responsableId(responsableId).enRetard(enRetard)
                .mesAudits(mesAudits).annee(annee).build(), pageable));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<AuditDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<AuditDto> create(@Valid @RequestBody AuditDto dto) {
        return ResponseEntity.ok(service.creer(dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<AuditDto> update(@PathVariable("id") UUID id, @Valid @RequestBody AuditDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/{id}/valider")
    public ResponseEntity<AuditDto> valider(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.valider(id));
    }

    /** Valide d'un geste une série d'audits : tout ou rien. */
    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/programme/valider")
    public ResponseEntity<ApiResponse<List<AuditDto>>> validerProgramme(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(ApiResponse.success(service.validerProgramme(ids)));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/demarrer")
    public ResponseEntity<AuditDto> demarrer(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.demarrer(id));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/annuler")
    public ResponseEntity<AuditDto> annuler(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.annuler(id));
    }

    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/{id}/cloturer")
    public ResponseEntity<AuditDto> cloturer(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.cloturer(id));
    }

    /** Le rapport d'audit (D14), structuré : seuls les constats publiés y entrent. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/rapport")
    public ResponseEntity<RapportAuditDto> rapport(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documents.rapport(id));
    }

    /** « Télécharger en PDF » / « Télécharger en Word » du rapport. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/rapport/export")
    public ResponseEntity<byte[]> exporterRapport(@PathVariable("id") UUID id,
                                                  @RequestParam(value = "format", defaultValue = "PDF") RenduDocument.Format format) {
        return telechargement(documents.rapportEnDocument(id, format));
    }

    /** « Télécharger en PDF » / « Télécharger en Word » du plan (B8). */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/plan/export")
    public ResponseEntity<byte[]> exporterPlan(@PathVariable("id") UUID id,
                                               @RequestParam(value = "format", defaultValue = "PDF") RenduDocument.Format format) {
        return telechargement(documents.planEnDocument(id, format));
    }

    private static ResponseEntity<byte[]> telechargement(DocumentsAuditService.Fichier fichier) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fichier.nom() + "\"")
                .contentType(MediaType.parseMediaType(fichier.typeMime()))
                .body(fichier.contenu());
    }

    /** Les conclusions et recommandations du rapport, tant qu'aucun visa n'est donné. */
    @PreAuthorize("@perm.canRead(this) and @perm.detient('audit-conduct', 'audit-write')")
    @PutMapping("/{id}/rapport/redaction")
    public ResponseEntity<AuditDto> rediger(@PathVariable("id") UUID id, @RequestBody RedactionRapportDto redaction) {
        return ResponseEntity.ok(service.rediger(id, redaction));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PostMapping(value = "/{id}/rapport/fichier", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuditDto> chargerRapport(@PathVariable("id") UUID id,
                                                   @RequestPart("fichier") MultipartFile fichier) {
        return ResponseEntity.ok(service.chargerRapport(id, fichier));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/rapport/fichier")
    public ResponseEntity<InputStreamResource> telechargerRapport(@PathVariable("id") UUID id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport-audit\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(service.contenuDuRapport(id)));
    }

    /**
     * Renvoie au module Non-conformités les écarts publiés de l'audit qui n'y ont pas encore de
     * dossier — après une panne de M05, ou un audit sans processus désigné, corrigé depuis.
     */
    @PreAuthorize("@perm.canRead(this) and @perm.detient('audit-conduct', 'audit-validate')")
    @PutMapping("/{id}/transmettre-ecarts")
    public ResponseEntity<TransmissionDto> transmettreEcarts(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(transmission.reprendre(service.lire(id).getId()));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/actions-risque/{actionId}/efficacite")
    public ResponseEntity<AuditDto> evaluerEfficacite(@PathVariable("id") UUID id,
                                                      @PathVariable("actionId") UUID actionId,
                                                      @RequestParam("niveau") NiveauEfficacite niveau) {
        return ResponseEntity.ok(service.evaluerEfficacite(id, actionId, niveau));
    }
}
