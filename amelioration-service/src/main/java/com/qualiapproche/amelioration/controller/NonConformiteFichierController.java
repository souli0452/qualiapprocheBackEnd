package com.qualiapproche.amelioration.controller;

import java.util.UUID;

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

import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.utils.EnTeteFichier;
import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import static com.qualiapproche.common.utils.ApiUrls.NON_CONFORMITE_ROOT_URL;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Fichiers attachés à une non-conformité.
 *
 * <p>Le dépôt est séparé de la décision de workflow qui s'en sert, et c'est volontaire : le moteur
 * ne transporte que des chaînes. Le client dépose d'abord le fichier, reçoit sa référence, puis
 * transmet cette référence comme valeur du champ {@code FILE} de l'étape — « déposer d'abord,
 * référencer ensuite ».</p>
 */
@RestController
@RequestMapping(NON_CONFORMITE_ROOT_URL + "/{ncId}/fichiers")
@RequiredArgsConstructor
@Tag(name = "Non-Conformités — fichiers", description = "Dépôt et consultation des fichiers d'une non-conformité")
@RequirePermissions(
        create = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        update = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        read = {"nc-read", "nc-write", "NC_READ", "CONSULTATION_NC", "SUBMIT_NC", "TRAITEMENT_NC"},
        delete = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        validate = {"nc-validate", "VALIDATION_RQ", "VALIDATION_CHEF"}
)
public class NonConformiteFichierController {

    private final NonConformiteFichierService fichierService;
    private final PieceJointeStockageService stockage;

    /**
     * Dépose un fichier et rend sa référence.
     *
     * <p>La réponse est enveloppée explicitement : {@code GlobalResponseHandler} laisse passer les
     * {@code String} sans les envelopper, et le client se retrouverait avec deux formes de réponse
     * selon le point d'entrée appelé.</p>
     */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Déposer un fichier sur une non-conformité",
            description = "Range le fichier sous non-conformite/<sigle de structure>/ et rend la référence de l'objet.")
    public ResponseEntity<ApiResponse<String>> deposer(
            @PathVariable("ncId") UUID ncId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(fichierService.deposer(ncId, file)));
    }

    /**
     * Télécharge un fichier déposé, désigné par sa référence.
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/contenu")
    @Operation(summary = "Télécharger un fichier d'une non-conformité")
    public ResponseEntity<Resource> telecharger(
            @PathVariable("ncId") UUID ncId,
            @RequestParam("reference") String reference) {

        PieceJointe pieceJointe = fichierService.pieceJointe(ncId, reference);
        Resource contenu = new InputStreamResource(stockage.contenu(pieceJointe));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, EnTeteFichier.attachement(pieceJointe.getNom()))
                .contentType(EnTeteFichier.typeDeContenu(pieceJointe.getType()))
                .body(contenu);
    }
}
