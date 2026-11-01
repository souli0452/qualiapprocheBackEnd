package com.qualiapproche.referentiel.plateforme;

import com.qualiapproche.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.QUALI_APPROCHE_ROOT_URL;

/**
 * La gestion des clients, réservée à l'administrateur de la plateforme (rôle de royaume
 * {@code PLATEFORME_ADMIN}).
 *
 * <p>Il crée les clients et en suit la licence ; il ne lit rien de leurs données — son jeton ne
 * porte aucune direction, et le cloisonnement ne lui montre rien.</p>
 */
@RestController
@RequestMapping(QUALI_APPROCHE_ROOT_URL + "/plateforme/clients")
@Tag(name = "Plateforme", description = "Clients de l'installation")
@PreAuthorize("hasRole('PLATEFORME_ADMIN')")
@RequiredArgsConstructor
public class PlateformeController {

    private final GestionDesClients gestion;

    @Operation(summary = "Les clients de l'installation et l'état de leur licence")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> lister() {
        // Enveloppé : GlobalResponseHandler paginerait une liste nue.
        List<GestionDesClients.Client> clients = gestion.clients();
        return ResponseEntity.ok(ApiResponse.success(Map.of("clients", clients)));
    }

    @Operation(summary = "Créer un client : sa direction, son paramétrage, son super administrateur")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> creer(@RequestBody GestionDesClients.NouveauClient demande) {
        return ResponseEntity.ok(ApiResponse.success(gestion.creer(demande)));
    }

    @Operation(summary = "Rejouer l'amorce du paramétrage d'un client dans chaque service")
    @PostMapping("/{direction}/amorce")
    public ResponseEntity<ApiResponse<Map<String, Object>>> amorcer(@PathVariable("direction") UUID direction) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("echecs", gestion.amorcer(direction))));
    }

    @Operation(summary = "Rendre à chaque client les lignes écrites avant le cloisonnement")
    @PostMapping("/rattrapage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rattraper() {
        return ResponseEntity.ok(ApiResponse.success(gestion.rattraper()));
    }
}
