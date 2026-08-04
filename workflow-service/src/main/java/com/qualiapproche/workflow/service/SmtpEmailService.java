package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.config.EmailTemplateEngineConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class SmtpEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine moteurGabarit;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Qualifier(EmailTemplateEngineConfig.MOTEUR_GABARIT_EMAIL) TemplateEngine moteurGabarit) {
        this.mailSender = mailSender;
        this.moteurGabarit = moteurGabarit;
    }

    /**
     * Envoie un courriel dont le corps est un gabarit Thymeleaf.
     *
     * <p>Le rendu se faisait par remplacement littéral de marqueurs {@code {{clé}}}. Or les
     * gabarits livrés sont tous écrits en Thymeleaf ({@code th:text}, {@code ${...}}) et aucun ne
     * contient un seul {@code {{}}} : aucune variable n'était donc jamais substituée. Les messages
     * partaient identiques pour tout le monde, attributs {@code th:} non interprétés compris, sans
     * mentionner ni le dossier ni l'étape concernés.</p>
     *
     * <p>Le passage à Thymeleaf apporte au passage l'échappement HTML des valeurs : un commentaire
     * de validation contenant du balisage était jusqu'ici recopié tel quel dans le message.</p>
     *
     * @param to           adresse du destinataire
     * @param subject      objet du message
     * @param htmlTemplate corps du gabarit, tel qu'enregistré en base
     * @param variables    valeurs exposées au gabarit ; une variable absente rend une chaîne vide
     */
    public void sendEmail(String to, String subject, String htmlTemplate, Map<String, String> variables) {
        String body = rendre(htmlTemplate, variables);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            mailSender.send(message);
            log.info("Email envoyé avec succès à {}", to);

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email à {}", to, e);
        }
    }

    /**
     * Rend le gabarit avec les variables fournies.
     *
     * <p>Un gabarit syntaxiquement invalide — il est éditable par API, donc faillible — n'empêche
     * pas l'envoi : le corps brut part alors sans substitution. Un message imparfait vaut mieux
     * qu'un responsable jamais prévenu.</p>
     */
    private String rendre(String htmlTemplate, Map<String, String> variables) {
        if (htmlTemplate == null || htmlTemplate.isBlank()) {
            return "";
        }
        try {
            Context contexte = new Context(Locale.FRENCH);
            if (variables != null) {
                contexte.setVariables(new HashMap<>(variables));
            }
            return moteurGabarit.process(htmlTemplate, contexte);
        } catch (Exception e) {
            log.error("Gabarit d'e-mail illisible, le corps est envoyé sans substitution : {}", e.getMessage());
            return htmlTemplate;
        }
    }
}
