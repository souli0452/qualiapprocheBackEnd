package com.qualiapproche.support.service;

import com.qualiapproche.support.model.WorkflowStepTemplate;
import com.qualiapproche.support.repository.WorkflowStepTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowStepTemplateService {

    private final WorkflowStepTemplateRepository stepTemplateRepository;

    public List<WorkflowStepTemplate> getAllTemplates() {
        return stepTemplateRepository.findAll();
    }

    public WorkflowStepTemplate getTemplateById(UUID id) {
        return stepTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Étape du catalogue introuvable avec l'ID: " + id));
    }

    @Transactional
    public WorkflowStepTemplate createTemplate(WorkflowStepTemplate template) {
        return stepTemplateRepository.save(template);
    }

    @Transactional
    public WorkflowStepTemplate updateTemplate(UUID id, WorkflowStepTemplate updated) {
        WorkflowStepTemplate existing = getTemplateById(id);
        existing.setNomEtape(updated.getNomEtape());
        existing.setResponsableRole(updated.getResponsableRole());
        existing.setDescription(updated.getDescription());
        return stepTemplateRepository.save(existing);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        stepTemplateRepository.deleteById(id);
    }
}
