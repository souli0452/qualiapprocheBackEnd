package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateDataInitializer implements CommandLineRunner {

    private final EmailTemplateRepository emailTemplateRepository;

    /**
     * Réécrit en base les gabarits livrés, au lieu de se contenter d'ajouter les manquants.
     *
     * <p>Une refonte des gabarits livrés n'atteint pas une installation déjà démarrée : les corps
     * vivent en base dès le premier lancement, et l'insertion seule ne les touche plus. Sans ce
     * drapeau, il faudrait rejouer une quinzaine de {@code PUT} à la main pour porter une nouvelle
     * version en production.</p>
     *
     * <p>Désactivé par défaut, et c'est délibéré : l'activer écrase les gabarits retouchés depuis
     * l'écran d'administration, sans possibilité de les retrouver. Il s'agit d'un geste
     * d'exploitation — on l'active le temps d'un redémarrage, puis on le remet à {@code false}.
     * Laissé actif, il rétablirait la version livrée à <b>chaque</b> démarrage, défaisant en
     * silence le travail de l'administrateur ; d'où la trace en {@code WARN} à chaque réécriture.</p>
     */
    @Value("${workflow.gabarits-courriel.rafraichir-au-demarrage:false}")
    private boolean rafraichirAuDemarrage;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Rapprochement par code, insertion seule.
        //
        // La condition portait sur le nombre total de modèles : dès qu'un seul existait, aucun
        // des modèles livrés ensuite n'atteignait plus la base — une notification restait alors
        // sans gabarit, et partait telle quelle. Un modèle retouché depuis l'écran n'est pas
        // davantage réécrit, sauf demande explicite : le corps de l'e-mail appartient à qui l'a
        // rédigé.
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
        int rafraichis = 0;
        for (TemplateDef def : definitions) {
            Optional<EmailTemplate> existant = emailTemplateRepository.findByCode(def.code);
            if (existant.isPresent() && !rafraichirAuDemarrage) {
                continue;
            }
            try {
                ClassPathResource resource = new ClassPathResource("templates/" + def.code + ".html");
                if (!resource.exists()) {
                    log.warn("Gabarit absent du classpath : templates/{}.html", def.code);
                    continue;
                }
                String body = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

                if (existant.isPresent()) {
                    if (rafraichir(existant.get(), def, body)) {
                        rafraichis++;
                    }
                    continue;
                }

                EmailTemplate template = EmailTemplate.builder()
                        .code(def.code)
                        .subject(def.subject)
                        .body(body)
                        .description(def.description)
                        .build();

                emailTemplateRepository.save(template);
                crees++;
                log.info("Modèle d'e-mail « {} » ajouté.", def.code);
            } catch (Exception e) {
                // Un gabarit illisible ne doit pas empêcher les autres d'être posés : sans cela,
                // un seul fichier fautif priverait de gabarit toutes les étapes suivantes.
                log.error("Modèle d'e-mail « {} » non chargé.", def.code, e);
            }
        }

        journaliser(crees, rafraichis, definitions.size());
    }

    /**
     * Rétablit le gabarit livré par-dessus celui de la base.
     *
     * <p>Écriture évitée quand rien ne diffère : un drapeau resté actif ne doit pas repousser les
     * quinze lignes à chaque démarrage, ni faire croire à une réécriture dans le journal.</p>
     *
     * @return {@code true} si la ligne a effectivement changé
     */
    private boolean rafraichir(EmailTemplate enBase, TemplateDef def, String body) {
        if (Objects.equals(enBase.getBody(), body)
                && Objects.equals(enBase.getSubject(), def.subject)
                && Objects.equals(enBase.getDescription(), def.description)) {
            return false;
        }
        enBase.setSubject(def.subject);
        enBase.setBody(body);
        enBase.setDescription(def.description);
        emailTemplateRepository.save(enBase);
        log.warn("Modèle d'e-mail « {} » rétabli dans sa version livrée : toute retouche faite "
                + "depuis l'écran d'administration est perdue.", def.code);
        return true;
    }

    private void journaliser(int crees, int rafraichis, int livres) {
        if (rafraichis > 0) {
            log.warn("Modèles d'e-mail : {} rétabli(s) dans leur version livrée sur {}. Remettez "
                    + "« workflow.gabarits-courriel.rafraichir-au-demarrage » à false : laissé actif, "
                    + "il défera à chaque démarrage les modèles retouchés depuis l'écran.",
                    rafraichis, livres);
        }
        if (crees > 0) {
            log.info("Modèles d'e-mail : {} ajouté(s) sur {} livré(s).", crees, livres);
        }
        if (crees == 0 && rafraichis == 0) {
            log.info("Modèles d'e-mail : les {} modèles livrés sont déjà présents et à jour.", livres);
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
