package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.qualiapproche.common.utils.ApiUrls.QUALI_APPROCHE_ROOT_URL;

/**
 * La licence de cette installation.
 *
 * <p>L'état est ouvert à tout utilisateur connecté : c'est lui qui commande l'écran d'accueil, et
 * le masquer ferait afficher une application vide à qui n'a pas les droits d'administration, sans
 * qu'il comprenne pourquoi.</p>
 *
 * <p>Installer une licence ou démarrer un essai reste réservé à l'administration : ce sont des
 * décisions qui engagent l'organisation.</p>
 */
@RestController
@RequestMapping(QUALI_APPROCHE_ROOT_URL + "/licence")
@Tag(name = "Licence", description = "Licence de l'installation : état, installation, essai")
@RequirePermissions(
        create = {"config-global-write", "CONFIG_GLOBAL_MANAGE"},
        update = {"config-global-write", "CONFIG_GLOBAL_MANAGE"},
        read = {"config-global-read", "config-global-write", "CONFIG_READ", "CONFIG_GLOBAL_MANAGE"},
        delete = {"config-global-write", "CONFIG_GLOBAL_MANAGE"}
)
@RequiredArgsConstructor
public class LicenceController {

    private final LicenceInstalleeService service;

    @Operation(summary = "État de la licence de cette installation")
    @GetMapping("/etat")
    public ResponseEntity<ApiResponse<EtatLicenceDto>> etat() {
        return ResponseEntity.ok(ApiResponse.success(service.etat()));
    }

    @Operation(summary = "Installer une licence remise par l'éditeur",
            description = "Le texte est vérifié avant d'être enregistré ; les modules s'ouvrent "
                    + "immédiatement, sans redémarrage.")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CONFIG_GLOBAL_MANAGE', 'config-global-write')")
    public ResponseEntity<ApiResponse<EtatLicenceDto>> installer(@RequestBody Map<String, String> corps) {
        return ResponseEntity.ok(ApiResponse.success(service.installer(corps.get("licence"))));
    }

    @Operation(summary = "Démarrer l'essai gratuit",
            description = "Une seule fois par installation, tous les modules ouverts.")
    @PostMapping("/essai")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CONFIG_GLOBAL_MANAGE', 'config-global-write')")
    public ResponseEntity<ApiResponse<EtatLicenceDto>> essai() {
        return ResponseEntity.ok(ApiResponse.success(service.demarrerEssai()));
    }
}
