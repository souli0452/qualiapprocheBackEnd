package com.qualiapproche.workflow.controller;

import com.qualiapproche.common.dto.EmailTemplateDto;
import com.qualiapproche.workflow.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @GetMapping
    public ResponseEntity<List<EmailTemplateDto>> getAllTemplates() {
        return ResponseEntity.ok(emailTemplateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(emailTemplateService.getTemplateById(id));
    }

    @PostMapping
    public ResponseEntity<EmailTemplateDto> createTemplate(@RequestBody EmailTemplateDto dto) {
        return ResponseEntity.ok(emailTemplateService.createTemplate(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> updateTemplate(@PathVariable UUID id, @RequestBody EmailTemplateDto dto) {
        return ResponseEntity.ok(emailTemplateService.updateTemplate(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        emailTemplateService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }
}
