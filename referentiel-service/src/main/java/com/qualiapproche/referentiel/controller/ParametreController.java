package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.ParametreDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.referentiel.service.ParametreService;
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
import java.util.Map;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.PARAMETRE_ROOT_URL;

/**
 * Réglages de l'organisation : contact, téléphone, logo, adresse — et ce que l'organisation y
 * ajoutera.
 *
 * <p>Ces réglages ont remplacé l'ancienne entité de configuration globale, dont les trois champs
 * figuraient en dur : ils y sont désormais trois clés parmi d'autres, et l'organisation en ajoute
 * sans qu'on livre du code. Les habilitations de l'ancien écran sont conservées, sans quoi un rôle
 * qui ne portait que {@code config-global-write} aurait perdu l'accès en silence.</p>
 *
 * <p>L'administration en est réservée à la configuration globale. La <b>lecture des valeurs
 * publiques</b>, elle, ne demande aucune permission : elle est appelée de service à service pour
 * composer le pied de page des courriels, souvent hors de toute requête utilisateur — donc sans
 * permission applicative à présenter. Ce que ce point d'entrée rend est précisément ce qui figure
 * déjà au bas d'un message envoyé à l'extérieur.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(PARAMETRE_ROOT_URL)
@RequirePermissions(
        create = {"config-global-write", "CONFIG_GLOBAL_MANAGE"},
        update = {"config-global-write", "CONFIG_GLOBAL_MANAGE"},
        read = {"config-global-read", "config-global-write", "CONFIG_READ", "CONFIG_GLOBAL_MANAGE"},
        delete = {"config-global-write", "CONFIG_GLOBAL_MANAGE"}
)
public class ParametreController {

    private final ParametreService service;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<ParametreDto> create(@RequestBody ParametreDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    /** La clé n'est pas modifiable : une clé différente est refusée en 409, pas ignorée. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<ParametreDto> update(@PathVariable("id") UUID id, @RequestBody ParametreDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Liste complète, rangée par clé.
     *
     * <p>Enveloppée explicitement : {@code GlobalResponseHandler} pagine d'office toute réponse de
     * type {@code List}, et l'écran d'administration n'aurait vu que les dix premiers réglages.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ParametreDto>>> getAll(
            @RequestParam(value = "recherche", required = false) String recherche) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(recherche)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/{id}")
    public ResponseEntity<ParametreDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/cle/{cle}")
    public ResponseEntity<ParametreDto> getByCle(@PathVariable("cle") String cle) {
        return ResponseEntity.ok(service.getByCle(cle));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Valeurs publiques, indexées par clé — sans habilitation requise.
     *
     * <p>Volontairement sans {@code @PreAuthorize} : le service qui compose un courriel appelle
     * après commit, sur un fil de fond, où aucune permission applicative ne circule. L'exiger
     * l'aurait fait échouer en 403 et le pied de page serait resté vide sans que rien ne l'explique.
     * Seuls les réglages marqués « lisible sans habilitation » y figurent, et seuls ceux qui portent
     * une valeur.</p>
     */
    @GetMapping("/publics")
    public ResponseEntity<ApiResponse<Map<String, String>>> valeursPubliques() {
        return ResponseEntity.ok(ApiResponse.success(service.valeursPubliques()));
    }
}
