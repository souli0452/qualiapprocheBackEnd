package com.qualiapproche.workflow.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envoie un email basé sur un template HTML dont les variables sont remplacées.
     * 
     * @param to L'adresse email du destinataire
     * @param subject L'objet de l'email
     * @param htmlTemplate Le corps HTML (avec des tags type {{variable}})
     * @param variables Map des variables à remplacer dans le template
     */
    public void sendEmail(String to, String subject, String htmlTemplate, Map<String, String> variables) {
        String body = htmlTemplate;
        
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                body = body.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }

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
}
