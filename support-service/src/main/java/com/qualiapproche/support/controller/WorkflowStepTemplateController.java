package com.qualiapproche.support.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.support.model.WorkflowStepTemplate;
import com.qualiapproche.support.service.WorkflowStepTemplateService;
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
 * Catalogue d'étapes réutilisables entre workflows (voir {@link com.qualiapproche.support.model.WorkflowStepTemplate}).
 * Rattaché à la même famille de permissions que les workflows eux-mêmes.
 */
@RestController
@RequestMapping("/api/v1/workflow-step-templates")
@RequiredArgsConstructor
@RequirePermissions(
        create = {"workflow-write"},
        update = {"workflow-write"},
        read = {"workflow-read", "workflow-write"},
        delete = {"workflow-write"}
)
public class WorkflowStepTemplateController {

    private final WorkflowStepTemplateService stepTemplateService;

    @GetMapping
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<List<WorkflowStepTemplate>> getAllTemplates() {
        return ResponseEntity.ok(stepTemplateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.canRead(this)")
    public ResponseEntity<WorkflowStepTemplate> getTemplateById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(stepTemplateService.getTemplateById(id));
    }

    @PostMapping
    @PreAuthorize("@perm.canCreate(this)")
    public ResponseEntity<WorkflowStepTemplate> createTemplate(@RequestBody WorkflowStepTemplate template) {
        return ResponseEntity.ok(stepTemplateService.createTemplate(template));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<WorkflowStepTemplate> updateTemplate(@PathVariable("id") UUID id, @RequestBody WorkflowStepTemplate template) {
        return ResponseEntity.ok(stepTemplateService.updateTemplate(id, template));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.canDelete(this)")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") UUID id) {
        stepTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
