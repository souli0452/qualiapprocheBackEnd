package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.service.impl.NonConformitesDAuditService;
import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.NonConformiteDAuditDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.common.utils.ApiUrls.NON_CONFORMITE_ROOT_URL;

/**
 * La porte d'entrée des écarts d'audit (M13) dans le module Non-conformités.
 *
 * <p>Ouverte aux auditeurs ({@code audit-conduct}, {@code audit-validate}) et non au seul
 * {@code nc-write} : c'est l'auditeur qui transmet l'écart en publiant son constat, et l'en
 * empêcher faute de droit sur les non-conformités laisserait l'écart sans suite. La porte ne
 * permet que cela — ouvrir un dossier depuis un audit, dans la structure auditée.</p>
 *
 * <p>Appelée de service à service, avec le jeton de l'auditeur : il est l'auteur du dossier.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(NON_CONFORMITE_ROOT_URL + "/depuis-audit")
@RequirePermissions(create = {"audit-conduct", "audit-validate", "nc-write"})
public class NonConformiteDAuditController {

    private final NonConformitesDAuditService service;

    /**
     * Ouvre la non-conformité d'un écart d'audit, et la soumet au pilote si demandé.
     *
     * <p>Rejouée avec la même {@code cleOrigine}, rend le dossier déjà ouvert sans le soumettre
     * une seconde fois. Création et soumission sont deux gestes, comme à la déclaration
     * ordinaire : la création doit être committée avant que le moteur ne revienne vers ce service.</p>
     */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping
    public ResponseEntity<NonConformiteDto> transmettre(@Valid @RequestBody NonConformiteDAuditDto ecart) {
        return service.dejaOuverte(ecart.getCleOrigine())
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    NonConformiteDto creee = service.ouvrir(ecart);
                    return ResponseEntity.ok(ecart.isSoumettre() ? service.soumettre(creee.getId()) : creee);
                });
    }
}
