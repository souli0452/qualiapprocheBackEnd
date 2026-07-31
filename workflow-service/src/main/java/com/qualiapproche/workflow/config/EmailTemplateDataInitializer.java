package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateDataInitializer implements CommandLineRunner {

    private final EmailTemplateRepository emailTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking for default Email Templates...");
        long count = emailTemplateRepository.count();

        if (count == 0) {
            log.info("No default Email Templates found. Initializing from common/resources...");
            initializeTemplates();
            log.info("Default Email Templates created successfully.");
        } else {
            log.info("Default Email Templates already exist (count: {}).", count);
        }
    }

    private void initializeTemplates() {
        List<TemplateDef> definitions = List.of(
            new TemplateDef("emailTemplate", "Nouvelle Non-Conformité imputée", "Attribution d'une NC à un utilisateur"),
            new TemplateDef("structureToStructure", "Non-Conformité transmise", "NC relevée par une structure envers une autre"),
            new TemplateDef("validationNonConformite", "Validation requise - Non-Conformité", "Validation hiérarchique requise"),
            new TemplateDef("rejectNonConformite", "Non-Conformité rejetée", "Rejet d'une NC"),
            new TemplateDef("emailPlanAction", "Nouveau plan d'action correctif", "Attribution d'un plan d'action correctif"),
            new TemplateDef("validationPlanRequise", "Validation requise - Plans d'actions", "Validation des plans d'actions par RQ"),
            new TemplateDef("validationRq", "Validation attendue - Non-Conformité", "Validation attendue par RQ"),
            new TemplateDef("emailRqPlan", "Mise en œuvre du plan d'action", "Info au RQ : plan mis en œuvre"),
            new TemplateDef("validationAfterPlan", "Validation requise - Clôture NC", "Validation pour clôture après plan"),
            new TemplateDef("succesTraitementNonformite", "Traitement NC réussi", "Traitement NC terminé avec succès"),
            new TemplateDef("traitementReussi", "Non-conformité traitée avec succès", "Traitement NC terminé (notification)"),
            new TemplateDef("alertePlanAction", "Alerte : échéance plan d'action", "Alerte sur échéance de plan d'action"),
            new TemplateDef("alerteLastDay", "Alerte : dernier jour échéance", "Alerte dernier jour avant échéance"),
            new TemplateDef("alerteEpuise", "Alerte : délai épuisé", "Alerte délai plan d'action épuisé"),
            new TemplateDef("rejectPlanAction", "Rejet d'un plan d'action", "Rejet d'un plan d'action")
        );

        for (TemplateDef def : definitions) {
            try {
                ClassPathResource resource = new ClassPathResource("templates/" + def.code + ".html");
                if (resource.exists()) {
                    String body = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                    
                    EmailTemplate template = EmailTemplate.builder()
                            .code(def.code)
                            .subject(def.subject)
                            .body(body)
                            .description(def.description)
                            .build();
                    
                    emailTemplateRepository.save(template);
                    log.info("Saved email template: {}", def.code);
                } else {
                    log.warn("Template file not found in classpath: templates/{}.html", def.code);
                }
            } catch (Exception e) {
                log.error("Failed to load template {}", def.code, e);
            }
        }
    }

    private static class TemplateDef {
        String code;
        String subject;
        String description;

        TemplateDef(String code, String subject, String description) {
            this.code = code;
            this.subject = subject;
            this.description = description;
        }
    }
}
