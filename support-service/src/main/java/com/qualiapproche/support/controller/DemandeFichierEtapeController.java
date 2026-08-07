package com.qualiapproche.support.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.support.service.FichiersDEtapeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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

import java.util.UUID;

/**
 * Pièces réclamées par une étape de circuit sur une demande de modification ou de suppression.
 *
 * <p>Le moteur ne transporte que des chaînes : la pièce est déposée d'abord, sa référence devient
 * ensuite la valeur du champ de l'étape — même convention que pour les non-conformités. Sans ce
 * point d'entrée, une étape documentaire réclamant un justificatif était indécidable : le champ ne
 * pouvait pas être renseigné, et l'écran n'affichait qu'un aveu d'impuissance.</p>
 *
 * <p>La lecture suffit à déposer, et c'est voulu : déposer une demande relève déjà de la lecture —
 * c'est parce qu'on ne peut pas modifier soi-même qu'on en fait la demande — et l'instruire peut
 * réclamer une pièce à qui n'écrit pas les documents.</p>
 */
@RestController
@RequestMapping("/api/v1/demandes-document/{demandeId}/fichiers-etape")
@RequiredArgsConstructor
@Tag(name = "Demandes — pièces d'étape",
        description = "Dépôt des pièces réclamées par une étape du circuit d'une demande")
@RequirePermissions(
        create = {"demande-document-read", "demande-document-write", "document-read", "document-write"},
        read = {"demande-document-read", "demande-document-write", "document-read", "document-write"}
)
public class DemandeFichierEtapeController {

    /** Segment de rangement : il figure dans la référence, et fonde le contrôle d'accès. */
    private static final String FAMILLE = "demandes";

    private final FichiersDEtapeService fichiers;

    /**
     * Dépose une pièce et rend sa référence.
     *
     * <p>Réponse enveloppée explicitement : {@code GlobalResponseHandler} laisse passer les
     * {@code String} sans les envelopper, et le client aurait deux formes de réponse selon le point
     * d'entrée appelé.</p>
     */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Déposer une pièce d'étape sur une demande")
    public ResponseEntity<ApiResponse<String>> deposer(
            @PathVariable("demandeId") UUID demandeId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(fichiers.deposer(FAMILLE, demandeId, file)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/contenu")
    @Operation(summary = "Télécharger une pièce d'étape d'une demande")
    public ResponseEntity<Resource> telecharger(
            @PathVariable("demandeId") UUID demandeId,
            @RequestParam("reference") String reference) {

        Resource contenu = new InputStreamResource(fichiers.contenu(FAMILLE, demandeId, reference));
        // Le nom du dépôt et son type déclaré : sans eux, le navigateur enregistrait un identifiant
        // technique sans extension exploitable.
        String type = fichiers.typeDeContenu(reference);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fichiers.nomPropose(reference) + "\"")
                .contentType(type != null ? MediaType.parseMediaType(type)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(contenu);
    }
}
