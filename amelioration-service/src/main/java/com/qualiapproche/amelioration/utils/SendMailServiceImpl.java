package com.qualiapproche.amelioration.utils;

import com.qualiapproche.common.utils.EmailMessage;
import com.qualiapproche.common.config.MailConfig;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.common.utils.CourrielParGabarit;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.qualiapproche.common.utils.ClesReglages.RESPONSABLE_QUALITE_EMAIL;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendMailServiceImpl implements SendMailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final MailConfig mailConfig;
    private final CourrielParGabarit courrielParGabarit;
    private final ReglagesOrganisation reglagesOrganisation;

    /**
     * Boîte authentifiée auprès du relais.
     *
     * <p>Un relais refuse un message dont l'expéditeur lui est étranger, et un message sans
     * expéditeur du tout. C'est pourquoi chaque envoi le pose, comme le fait déjà user-service.</p>
     */
    private String expediteur() {
        return mailConfig.getUsername();
    }

    @Override
    public void sendVerificationEmail(String recipientEmail, String firstName, String lastName, String password,
            String url) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(expediteur());
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
            helper.setFrom(expediteur());
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
            helper.setFrom(expediteur());
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
    public void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link,
            String templateName, String fullName, String numeroNc, String observation) {
        EmailMessage emailMessage = EmailMessage.builder()
                .subject(subject)
                .toAddress(currentUserEmail)
                .ccAddress(courrielDuResponsableQualite())
                .build();

        Map<String, Object> variables = new HashMap<>();
        variables.put("link", link);
        variables.put("fullName", fullName);
        variables.put("numeroNc", numeroNc);
        variables.put("observation", observation);

        // Même expéditeur, même session, mêmes réglages que les autres envois : l'ancien utilitaire
        // ouvrait sa propre session JavaMail, avec son propre chiffrement. Les MailException
        // remontent — les emballer dans un RuntimeException nu privait l'appelant de la cause.
        courrielParGabarit.envoyer(emailMessage, variables, Collections.emptyList(),
                templateName, templateEngine);
    }

    /**
     * Courriel du responsable qualité, mis en copie des imputations, ou {@code null}.
     *
     * <p>Le responsable qualité doit être en copie de tout courriel sortant d'une non-conformité :
     * c'est lui qui en pilote le traitement. L'adresse est un réglage de l'organisation, où la
     * configuration globale a été reversée.</p>
     *
     * <p>Une copie manquante ne doit pas empêcher le destinataire principal d'être prévenu :
     * référentiel injoignable ou réglage non renseigné, le message part sans copie, et le journal le
     * dit.</p>
     */
    private String courrielDuResponsableQualite() {
        String courriel = reglagesOrganisation.valeur(RESPONSABLE_QUALITE_EMAIL);
        if (courriel == null) {
            log.warn("Le responsable qualité doit être en copie des courriels de non-conformité, mais "
                    + "le réglage « {} » n'est pas renseigné : le message part sans copie.",
                    RESPONSABLE_QUALITE_EMAIL);
        }
        return courriel;
    }

    @Override
    public void sendMail(String recipientEmail, String subject, String message, boolean isHtml) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(expediteur());
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(message, isHtml);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
}
