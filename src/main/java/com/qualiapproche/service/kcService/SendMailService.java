package com.qualiapproche.service.kcService;


import com.qualiapproche.config.utils.MailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SendMailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final MailConfig mailConfig;





    public void sendVerificationEmail(String recipientEmail, String firstName, String lastName, String password ,String url) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipientEmail);
            helper.setSubject("Vérification de votre compte");

            Map<String, Object> variables = new HashMap<>();
            variables.put("firstName", firstName);
            variables.put("lastName", lastName);
            variables.put("temporaryPassword", password);
            variables.put("verificationLink", url);

            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("verificationEmailTemplate.html", context);

            helper.setText(htmlContent, true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    public void sendReinitializePasswordEmail(String recipientEmail, String resetUrl) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipientEmail);
            helper.setSubject("Réinitialisation de votre mot de passe");

            Map<String, Object> variables = new HashMap<>();
            variables.put("resetLink", resetUrl);

            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("reinitializePasswordEmailTemplate.html", context);
            helper.setText(htmlContent, true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }


    public void sendResetPasswordEmail(String recipientEmail, String temporaryPassword, String url) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipientEmail);
            helper.setSubject("Votre nouveau mot de passe temporaire");

            Map<String, Object> variables = new HashMap<>();
            variables.put("temporaryPassword", temporaryPassword);
            variables.put("url", url);

            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("resetPasswordEmailTemplate.html", context);

            helper.setText(htmlContent, true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

}


