package com.qualiapproche.workflow.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.EmailTemplateDto;
import com.qualiapproche.workflow.service.EmailTemplateService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Modèles des courriels d'étape.
 *
 * <p>Un modèle n'est pas une donnée comme une autre : c'est un gabarit Thymeleaf que le serveur
 * <b>évalue</b> à chaque franchissement d'étape, puis envoie sous le nom de l'organisation. Laissé
 * ouvert à tout compte authentifié, il permettait de réécrire les courriels que reçoivent les
 * responsables — un lien de dossier remplacé par une adresse de son choix — et de faire évaluer
 * au serveur les expressions de son choix. Il relève donc de la même habilitation que les circuits
 * qui le citent : {@code workflow-write}, celle de l'administration des circuits
 * ({@link WorkflowController}).</p>
 *
 * <p>La lecture est réservée aux permissions du paramétrage des circuits : le front ne la sert
 * qu'à l'écran des modèles et à l'éditeur de circuit, tous deux derrière {@code workflow-read} ou
 * {@code workflow-write}. Aucun service ne la lit par Feign — le moteur lit les modèles en base.</p>
 */
@RestController
@RequestMapping("/api/v1/email-templates")
@RequiredArgsConstructor
@RequirePermissions(
        create = {"workflow-write"},
        update = {"workflow-write"},
        delete = {"workflow-write"},
        read = {"workflow-read", "workflow-write"}
)
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @GetMapping
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<EmailTemplateDto>> getAllTemplates() {
        return ResponseEntity.ok(emailTemplateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<EmailTemplateDto> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(emailTemplateService.getTemplateById(id));
    }

    @PostMapping
    @PreAuthorize("@perm.canCreate(this)")
    public ResponseEntity<EmailTemplateDto> createTemplate(@RequestBody EmailTemplateDto dto) {
        return ResponseEntity.ok(emailTemplateService.createTemplate(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<EmailTemplateDto> updateTemplate(@PathVariable UUID id, @RequestBody EmailTemplateDto dto) {
        return ResponseEntity.ok(emailTemplateService.updateTemplate(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.canDelete(this)")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        emailTemplateService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }
}
