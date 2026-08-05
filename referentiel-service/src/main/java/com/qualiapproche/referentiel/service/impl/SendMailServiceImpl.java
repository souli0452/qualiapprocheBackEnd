package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.service.SendMailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendMailServiceImpl implements SendMailService {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendVerificationEmail(String recipientEmail, String firstName, String lastName, String password, String url) {
        // Not used in referentiel
    }

    @Override
    public void sendReinitializePasswordEmail(String recipientEmail, String resetUrl) {
        // Not used in referentiel
    }

    @Override
    public void sendResetPasswordEmail(String recipientEmail, String temporaryPassword, String url) {
        // Not used in referentiel
    }

    @Override
    public void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link, String templateName, String fullName,
            String numeroNc, String observation) {
        // Not used in referentiel
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
