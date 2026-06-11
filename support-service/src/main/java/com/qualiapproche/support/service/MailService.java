package com.qualiapproche.support.service;

import com.qualiapproche.common.config.MailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service d'envoi d'emails du support-service.
 * Utilise JavaMailSender + Thymeleaf pour les templates HTML.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailConfig mailConfig;

    /**
     * Envoie un email HTML basé sur un template Thymeleaf de manière asynchrone.
     *
     * @param to           Adresse email du destinataire
     * @param subject      Sujet de l'email
     * @param templateName Nom du template (sans chemin, ex: "qmsDocumentShare")
     * @param variables    Variables à injecter dans le template
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (to == null || to.isBlank()) {
            log.warn("Envoi d'email ignoré : adresse destinataire vide. Sujet: {}", subject);
            return;
        }

        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailConfig.getUsername());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Email '{}' envoyé avec succès à {}", subject, to);
        } catch (MessagingException e) {
            log.error("Échec de l'envoi d'email '{}' à {}: {}", subject, to, e.getMessage(), e);
        }
    }
}
