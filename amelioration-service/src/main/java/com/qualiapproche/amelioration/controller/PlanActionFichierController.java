package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.service.impl.PlanActionFichierService;
import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.PieceJointeDTO;
import com.qualiapproche.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.PLAN_ACTION_ROOT_URL;

/**
 * Fichiers justificatifs d'un plan d'action.
 *
 * <p>Le contenu se relit et se supprime par {@code PieceJointeController}, commun au module : une
 * pièce est désignée par son identifiant, quel que soit le dossier auquel elle est rattachée.</p>
 */
@RestController
@RequestMapping(PLAN_ACTION_ROOT_URL + "/{planId}/fichiers")
@RequiredArgsConstructor
@Tag(name = "Plans d'action — fichiers", description = "Dépôt des justificatifs d'une action corrective")
@RequirePermissions(
        create = {"plan-action-write", "TRAITEMENT_PLAN"},
        update = {"plan-action-write", "TRAITEMENT_PLAN"},
        read = {"plan-action-read", "plan-action-write", "ACTIONS_READ", "TRAITEMENT_PLAN"},
        delete = {"plan-action-write", "TRAITEMENT_PLAN"},
        validate = {"plan-action-validate", "TRAITEMENT_PLAN"}
)
public class PlanActionFichierController {

    private final PlanActionFichierService fichierService;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Déposer un justificatif sur un plan d'action")
    public ResponseEntity<PieceJointeDTO> deposer(
            @PathVariable("planId") UUID planId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(fichierService.deposer(planId, file));
    }

    /**
     * Justificatifs du plan, enveloppés explicitement.
     *
     * <p>{@code GlobalResponseHandler} pagine d'office toute réponse de type {@code List}, à dix
     * éléments : le tableau attendu serait devenu un objet paginé, et l'écran n'aurait affiché
     * qu'une partie des pièces sans que rien ne le signale.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    @Operation(summary = "Lister les justificatifs d'un plan d'action")
    public ResponseEntity<ApiResponse<List<PieceJointeDTO>>> pieces(@PathVariable("planId") UUID planId) {
        return ResponseEntity.ok(ApiResponse.success(fichierService.pieces(planId)));
    }
}
