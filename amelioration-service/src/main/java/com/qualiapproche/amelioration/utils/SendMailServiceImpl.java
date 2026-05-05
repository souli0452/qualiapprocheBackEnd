package com.qualiapproche.amelioration.utils;

import com.qualiapproche.common.utils.EmailMessage;
import com.qualiapproche.common.config.MailConfig;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.common.utils.MailUtils;
import com.qualiapproche.referentiel.repository.ConfigGlobalRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SendMailServiceImpl implements SendMailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final MailConfig mailConfig;
    private final ConfigGlobalRepository configGlobalRepository;

    @Override
    public void sendVerificationEmail(String recipientEmail, String firstName, String lastName, String password, String url) {
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

    @Override
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

    @Override
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

    @Override
    public void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link, String templateName, String fullName, String numeroNc, String observation) {
        EmailMessage emailMessage = EmailMessage.builder()
                .subject(subject)
                .to_address(currentUserEmail)
                .cc_address(configGlobalRepository.findAll().stream().findFirst()
                        .map(c -> c.getEmailRq()).orElse(null))
                .build();

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("link", link);
            variables.put("fullName", fullName);
            variables.put("numeroNc", numeroNc);
            variables.put("observation", observation);

            MailUtils.sendEmailWithTheamleafEngine(
                    emailMessage,
                    mailConfig,
                    variables,
                    Collections.emptyList(),
                    templateName
            );
        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMail(String recipientEmail, String subject, String message, boolean isHtml) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(message, isHtml);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
}
