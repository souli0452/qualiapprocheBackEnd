package com.qualiapproche.support.service;

import com.qualiapproche.support.model.WorkflowStepTemplate;
import com.qualiapproche.support.repository.WorkflowStepTemplateRepository;
import com.qualiapproche.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
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
        String code = template.getCode() != null && !template.getCode().isBlank()
                ? normaliserCode(template.getCode())
                : normaliserCode(template.getNomEtape());

        stepTemplateRepository.findByCode(code).ifPresent(existant -> {
            throw new BusinessException(
                    "Le code « " + code + " » est déjà utilisé par l'étape de catalogue « "
                            + existant.getNomEtape() + " ». Chaque entrée doit porter un code distinct.",
                    HttpStatus.CONFLICT);
        });

        template.setCode(code);
        return stepTemplateRepository.save(template);
    }

    /**
     * Le code n'est volontairement pas modifiable : les circuits déjà assemblés à partir de cette
     * entrée en ont hérité, et le changer ici les désolidariserait du catalogue sans que rien ne
     * le signale. Le libellé, le rôle et la description restent librement modifiables.
     */
    @Transactional
    public WorkflowStepTemplate updateTemplate(UUID id, WorkflowStepTemplate updated) {
        WorkflowStepTemplate existing = getTemplateById(id);

        if (updated.getCode() != null && !updated.getCode().isBlank()
                && !normaliserCode(updated.getCode()).equals(existing.getCode())) {
            throw new BusinessException(
                    "Le code d'une étape de catalogue ne peut pas être modifié ("
                            + existing.getCode() + " → " + normaliserCode(updated.getCode())
                            + "). Créez une nouvelle entrée si nécessaire.",
                    HttpStatus.CONFLICT);
        }

        existing.setNomEtape(updated.getNomEtape());
        existing.setResponsableRole(updated.getResponsableRole());
        existing.setDescription(updated.getDescription());
        return stepTemplateRepository.save(existing);
    }

    /** Même normalisation que côté workflow-service : majuscules, sans accents, souligné. */
    private String normaliserCode(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return "ETAPE";
        }
        String normalise = java.text.Normalizer.normalize(valeur, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (normalise.isBlank()) {
            return "ETAPE";
        }
        return normalise.length() > 60 ? normalise.substring(0, 60) : normalise;
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        stepTemplateRepository.deleteById(id);
    }
}
