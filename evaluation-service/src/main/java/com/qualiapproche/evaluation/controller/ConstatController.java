package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.dto.CompilationDto;
import com.qualiapproche.evaluation.dto.ConstatDto;
import com.qualiapproche.evaluation.dto.MotifDto;
import com.qualiapproche.evaluation.dto.RepartitionPointDto;
import com.qualiapproche.evaluation.dto.PointControleDto;
import com.qualiapproche.evaluation.dto.PreuveDto;
import com.qualiapproche.evaluation.entities.PreuveConstat;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.qualiapproche.evaluation.service.ConstatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.evaluation.controller.AuditUrls.CONSTATS;

/**
 * Les constats.
 *
 * <p>La permission ouvre la porte ; l'appartenance à l'équipe de l'audit et la propriété du
 * brouillon, vérifiées par le service, décident du reste. Valider en réunion d'équipe revient à
 * l'équipe, ou à qui détient {@code audit-validate}.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(CONSTATS)
@RequirePermissions(
        create = {"audit-conduct"},
        update = {"audit-conduct"},
        read = {"audit-read", "audit-conduct", "audit-validate"},
        delete = {"audit-conduct"},
        validate = {"audit-conduct", "audit-validate"}
)
public class ConstatController {

    private final ConstatService service;
    private final com.qualiapproche.evaluation.service.RepartitionDesPoints repartition;

    /** Ce que l'équipe voit d'un audit : les constats compilés et publiés. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ConstatDto>>> compilation(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.compilation(auditId)));
    }

    /** « Mes constats du jour » : les brouillons de l'appelant. */
    @PreAuthorize("@perm.canCreate(this)")
    @GetMapping("/mes-brouillons")
    public ResponseEntity<ApiResponse<List<ConstatDto>>> mesBrouillons(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.mesBrouillons(auditId)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<ConstatDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.lire(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<ConstatDto> create(@RequestParam("auditId") UUID auditId, @Valid @RequestBody ConstatDto dto) {
        return ResponseEntity.ok(service.creer(auditId, dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<ConstatDto> update(@PathVariable("id") UUID id, @Valid @RequestBody ConstatDto dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/preuves")
    public ResponseEntity<ApiResponse<List<PreuveDto>>> preuves(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.preuves(id)));
    }

    /** Joint des éléments de preuve ; 503 si l'installation n'a pas de serveur de fichiers. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PostMapping(value = "/{id}/preuves", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<PreuveDto>>> joindre(@PathVariable("id") UUID id,
                                                                @RequestPart("fichiers") List<MultipartFile> fichiers) {
        return ResponseEntity.ok(ApiResponse.success(service.joindre(id, fichiers)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/preuves/{preuveId}")
    public ResponseEntity<InputStreamResource> telechargerPreuve(@PathVariable("preuveId") UUID preuveId) {
        PreuveConstat preuve = service.preuve(preuveId);
        String nom = preuve.getNom() == null ? "preuve" : preuve.getNom().replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, EnTeteFichier.attachement(nom))
                .contentType(preuve.getType() == null ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(preuve.getType()))
                .body(new InputStreamResource(service.contenu(preuve)));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @DeleteMapping("/preuves/{preuveId}")
    public ResponseEntity<Void> retirerPreuve(@PathVariable("preuveId") UUID preuveId) {
        service.retirerPreuve(preuveId);
        return ResponseEntity.noContent().build();
    }

    /** Le rédacteur verse son brouillon à la compilation d'équipe. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/publier")
    public ResponseEntity<ConstatDto> publier(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.publier(id));
    }

    /** L'équipe renvoie un constat compilé à son auteur. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{id}/retirer")
    public ResponseEntity<ConstatDto> retirer(@PathVariable("id") UUID id, @RequestBody(required = false) MotifDto motif) {
        return ResponseEntity.ok(service.retirerVersBrouillon(id, motif == null ? null : motif.getMotif()));
    }

    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/{id}/valider")
    public ResponseEntity<ConstatDto> valider(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.valider(id));
    }

    /**
     * Valide les constats compilés : ceux d'une nature (« Valider ce groupe »), ou tous sans
     * {@code natureId} (« L'équipe est d'accord — valider tout »).
     */
    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping("/valider-compilation")
    public ResponseEntity<ApiResponse<List<ConstatDto>>> validerCompilation(
            @RequestParam("auditId") UUID auditId,
            @RequestParam(value = "natureId", required = false) UUID natureId) {
        return ResponseEntity.ok(ApiResponse.success(service.validerLaCompilation(auditId, natureId)));
    }

    /** Le bandeau de la compilation : totaux, et décompte par nature. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/synthese")
    public ResponseEntity<CompilationDto> synthese(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(service.synthese(auditId));
    }

    /** « Publier tous mes constats vers la compilation d'équipe ». */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/publier-mes-constats")
    public ResponseEntity<ApiResponse<List<ConstatDto>>> publierMesConstats(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.publierTousMesConstats(auditId)));
    }

    /** Les points de contrôle assignés à l'appelant sur les checklists de l'audit. */
    @PreAuthorize("@perm.canCreate(this)")
    @GetMapping("/mes-points")
    public ResponseEntity<ApiResponse<List<PointControleDto>>> mesPoints(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(service.mesPointsDeControle(auditId)));
    }

    /** Qui renseigne quel point de contrôle sur cet audit (observation 20). */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/repartition")
    public ResponseEntity<ApiResponse<List<RepartitionPointDto>>> repartition(@RequestParam("auditId") UUID auditId) {
        return ResponseEntity.ok(ApiResponse.success(repartition.repartition(auditId)));
    }

    /** Le responsable d'équipe répartit les points entre les membres ; un point sans auditeur revient à toute l'équipe. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/repartition")
    public ResponseEntity<ApiResponse<List<RepartitionPointDto>>> repartir(@RequestParam("auditId") UUID auditId,
                                                                          @RequestBody List<RepartitionPointDto> saisie) {
        return ResponseEntity.ok(ApiResponse.success(repartition.repartir(auditId, saisie)));
    }
}
