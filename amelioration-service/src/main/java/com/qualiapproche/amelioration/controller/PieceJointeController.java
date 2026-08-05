package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.utils.EnTeteFichier;
import com.qualiapproche.common.annotation.RequirePermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.QUALI_APPROCHE_ROOT_URL;

/**
 * Contenu des pièces jointes du module amélioration.
 *
 * <p>Les listes de dossiers ne portent plus que la description des pièces — nom, type, référence.
 * Elles renvoyaient auparavant le contenu de chaque fichier, encodé en base64 : chaque page de
 * chaque liste rapatriait ainsi tous les fichiers de toutes ses lignes. Le contenu se lit
 * désormais ici, quand l'utilisateur le demande.</p>
 */
@RestController
@RequestMapping(QUALI_APPROCHE_ROOT_URL + "/pieces-jointes")
@RequiredArgsConstructor
@Tag(name = "Pièces jointes", description = "Téléchargement des fichiers des non-conformités et plans d'action")
@RequirePermissions(
        create = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        update = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        read = {"nc-read", "nc-write", "NC_READ", "CONSULTATION_NC", "SUBMIT_NC", "TRAITEMENT_NC"},
        delete = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        validate = {"nc-validate", "VALIDATION_RQ", "VALIDATION_CHEF"}
)
public class PieceJointeController {

    private final PieceJointeStockageService stockage;

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}/contenu")
    @Operation(summary = "Télécharger une pièce jointe")
    public ResponseEntity<Resource> telecharger(@PathVariable("id") UUID id) {
        PieceJointe piece = stockage.piece(id);
        Resource contenu = new InputStreamResource(stockage.contenu(piece));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, EnTeteFichier.attachement(piece.getNom()))
                .contentType(EnTeteFichier.typeDeContenu(piece.getType()))
                .body(contenu);
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une pièce jointe, contenu compris")
    public ResponseEntity<Void> supprimer(@PathVariable("id") UUID id) {
        stockage.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
