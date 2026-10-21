package com.qualiapproche.userservice.service.impl;

import com.qualiapproche.common.messagerie.Courriels;
import com.qualiapproche.common.service.SendMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Courriels des comptes (vérification d'adresse, réinitialisation, mot de passe temporaire) :
 * rendus par leur gabarit, puis déposés dans la boîte d'envoi du service ({@link Courriels}), que la
 * tournée remet au serveur de messagerie.
 *
 * <p>Ils partaient directement depuis la requête : une panne du serveur de messagerie perdait le
 * lien de réinitialisation, et l'utilisateur ne le recevait jamais. Déposés dans la
 * transaction de l'appelant, ils n'existent que si elle est validée, et sont rejoués jusqu'à leur
 * remise.</p>
 *
 * <p>Ceux qui portent un mot de passe ou un lien à jeton sont confidentiels : leur contenu est
 * effacé de la boîte d'envoi dès qu'il est parti.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendMailServiceImpl implements SendMailService {

    private final TemplateEngine templateEngine;
    private final Courriels courriels;

    @Override
    public void sendVerificationEmail(String recipientEmail, String firstName, String lastName, String password,
            String url) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("temporaryPassword", password);
        variables.put("verificationLink", url);
        deposerConfidentiel(recipientEmail, "Vérification de votre compte", "verificationEmailTemplate.html", variables);
    }

    @Override
    public void sendReinitializePasswordEmail(String recipientEmail, String resetUrl) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("resetLink", resetUrl);
        deposerConfidentiel(recipientEmail, "Réinitialisation de votre mot de passe",
                "reinitializePasswordEmailTemplate.html", variables);
    }

    @Override
    public void sendResetPasswordEmail(String recipientEmail, String temporaryPassword, String url) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("temporaryPassword", temporaryPassword);
        variables.put("url", url);
        deposerConfidentiel(recipientEmail, "Votre nouveau mot de passe temporaire", "resetPasswordEmailTemplate.html",
                variables);
    }

    /** Sans copie au responsable qualité : user-service n'a pas accès aux réglages de l'organisation. */
    @Override
    public void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link,
            String templateName, String fullName, String numeroNc, String observation) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("link", link);
        variables.put("fullName", fullName);
        variables.put("numeroNc", numeroNc);
        variables.put("observation", observation);
        deposer(currentUserEmail, null, subject, templateName, variables);
    }

    @Override
    public void sendMail(String recipientEmail, String subject, String message, boolean isHtml) {
        String corps = message == null ? "" : message;
        if (!isHtml) {
            // La file ne garde que du HTML : un texte brut y entre échappé, ses lignes préservées.
            corps = HtmlUtils.htmlEscape(corps).replace("\n", "<br>");
        }
        courriels.envoyer(recipientEmail, null, subject, corps, "message");
    }

    /**
     * Un courriel qui porte un mot de passe ou un lien à jeton : il part comme les autres, mais son
     * contenu ne reste pas dans la boîte d'envoi une fois publié.
     */
    private void deposerConfidentiel(String destinataire, String sujet, String gabarit, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        courriels.envoyerConfidentiel(destinataire, sujet, templateEngine.process(gabarit, context), gabarit);
    }

    private void deposer(String destinataire, String copie, String sujet, String gabarit, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        courriels.envoyer(destinataire, copie, sujet, templateEngine.process(gabarit, context), gabarit);
    }
}
