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
        // Rapprochement par code, insertion seule.
        //
        // La condition portait sur le nombre total de modèles : dès qu'un seul existait, aucun
        // des modèles livrés ensuite n'atteignait plus la base — une notification restait alors
        // sans gabarit, et partait telle quelle. Un modèle retouché depuis l'écran n'est pas
        // davantage réécrit : le corps de l'e-mail appartient à qui l'a rédigé.
        initializeTemplates();
    }

    private void initializeTemplates() {
        // {numeroNc} dans l'objet : la même syntaxe {variable} que le corps, substituée à l'envoi.
        // Une référence absente s'efface avec son séparateur — l'objet reste propre.
        List<TemplateDef> definitions = List.of(
            new TemplateDef("emailTemplate", "Nouvelle Non-Conformité imputée – {numeroNc}", "Attribution d'une NC à un utilisateur"),
            new TemplateDef("structureToStructure", "Non-Conformité transmise – {numeroNc}", "NC relevée par une structure envers une autre"),
            new TemplateDef("validationNonConformite", "Validation requise - Non-Conformité {numeroNc}", "Validation hiérarchique requise"),
            new TemplateDef("rejectNonConformite", "Non-Conformité {numeroNc} rejetée", "Rejet d'une NC"),
            new TemplateDef("emailPlanAction", "Nouveau plan d'action correctif – {numeroNc}", "Attribution d'un plan d'action correctif"),
            new TemplateDef("validationPlanRequise", "Validation requise - Plans d'actions – {numeroNc}", "Validation des plans d'actions par RQ"),
            new TemplateDef("validationRq", "Validation attendue - Non-Conformité {numeroNc}", "Validation attendue par RQ"),
            new TemplateDef("emailRqPlan", "Mise en œuvre du plan d'action – {numeroNc}", "Info au RQ : plan mis en œuvre"),
            new TemplateDef("validationAfterPlan", "Validation requise - Clôture NC {numeroNc}", "Validation pour clôture après plan"),
            new TemplateDef("succesTraitementNonformite", "Traitement NC {numeroNc} réussi", "Traitement NC terminé avec succès"),
            new TemplateDef("traitementReussi", "Non-conformité {numeroNc} traitée avec succès", "Traitement NC terminé (notification)"),
            new TemplateDef("alertePlanAction", "Alerte : échéance plan d'action", "Alerte sur échéance de plan d'action"),
            new TemplateDef("alerteLastDay", "Alerte : dernier jour échéance", "Alerte dernier jour avant échéance"),
            new TemplateDef("alerteEpuise", "Alerte : délai épuisé", "Alerte délai plan d'action épuisé"),
            new TemplateDef("rejectPlanAction", "Rejet d'un plan d'action – {numeroNc}", "Rejet d'un plan d'action")
        );

        int crees = 0;
        for (TemplateDef def : definitions) {
            if (emailTemplateRepository.findByCode(def.code).isPresent()) {
                continue;
            }
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
                    crees++;
                    log.info("Modèle d'e-mail « {} » ajouté.", def.code);
                } else {
                    log.warn("Template file not found in classpath: templates/{}.html", def.code);
                }
            } catch (Exception e) {
                log.error("Failed to load template {}", def.code, e);
            }
        }

        if (crees > 0) {
            log.info("Modèles d'e-mail : {} ajouté(s) sur {} livré(s).", crees, definitions.size());
        } else {
            log.info("Modèles d'e-mail : les {} modèles livrés sont déjà présents.", definitions.size());
        }
    }

    private static class TemplateDef {
        private final String code;
        private final String subject;
        private final String description;

        TemplateDef(String code, String subject, String description) {
            this.code = code;
            this.subject = subject;
            this.description = description;
        }
    }
}
