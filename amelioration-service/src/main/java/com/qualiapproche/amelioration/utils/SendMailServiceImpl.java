package com.qualiapproche.amelioration.utils;

import com.qualiapproche.amelioration.courriel.CourrielSortant;
import com.qualiapproche.common.courriel.FileDeCourriels;
import com.qualiapproche.common.service.SendMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static com.qualiapproche.common.utils.ClesReglages.RESPONSABLE_QUALITE_EMAIL;

/**
 * Courriels du module Non-conformités : rendus par leur gabarit, puis déposés dans la file du
 * service ({@link FileDeCourriels}), que la tournée remet au serveur de messagerie.
 *
 * <p>Ils partaient directement, depuis la requête ou la tournée de nuit : une panne du serveur de
 * messagerie faisait échouer le geste de l'utilisateur, ou perdait la relance. Déposés dans la
 * transaction de l'appelant, ils n'existent que si elle est validée, et sont rejoués jusqu'à leur
 * remise.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendMailServiceImpl implements SendMailService {

    private final TemplateEngine templateEngine;
    private final FileDeCourriels<CourrielSortant> file;
    private final ReglagesOrganisation reglagesOrganisation;

    @Override
    public void sendVerificationEmail(String recipientEmail, String firstName, String lastName, String password,
            String url) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("lastName", lastName);
        variables.put("temporaryPassword", password);
        variables.put("verificationLink", url);
        deposer(recipientEmail, null, "Vérification de votre compte", "verificationEmailTemplate.html", variables);
    }

    @Override
    public void sendReinitializePasswordEmail(String recipientEmail, String resetUrl) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("resetLink", resetUrl);
        deposer(recipientEmail, null, "Réinitialisation de votre mot de passe",
                "reinitializePasswordEmailTemplate.html", variables);
    }

    @Override
    public void sendResetPasswordEmail(String recipientEmail, String temporaryPassword, String url) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("temporaryPassword", temporaryPassword);
        variables.put("url", url);
        deposer(recipientEmail, null, "Votre nouveau mot de passe temporaire", "resetPasswordEmailTemplate.html",
                variables);
    }

    /** Le responsable qualité est en copie : c'est lui qui pilote le traitement des non-conformités. */
    @Override
    public void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link,
            String templateName, String fullName, String numeroNc, String observation) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("link", link);
        variables.put("fullName", fullName);
        variables.put("numeroNc", numeroNc);
        variables.put("observation", observation);
        deposer(currentUserEmail, courrielDuResponsableQualite(), subject, templateName, variables);
    }

    @Override
    public void sendMail(String recipientEmail, String subject, String message, boolean isHtml) {
        String corps = message == null ? "" : message;
        if (!isHtml) {
            // La file ne garde que du HTML : un texte brut y entre échappé, ses lignes préservées.
            corps = HtmlUtils.htmlEscape(corps).replace("\n", "<br>");
        }
        file.deposer(recipientEmail, null, subject, corps, "message");
    }

    private void deposer(String destinataire, String copie, String sujet, String gabarit, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        file.deposer(destinataire, copie, sujet, templateEngine.process(gabarit, context), gabarit);
    }

    /**
     * Courriel du responsable qualité, mis en copie des imputations, ou {@code null}.
     *
     * <p>Une copie manquante ne doit pas empêcher le destinataire principal d'être prévenu :
     * réglage non renseigné, le message part sans copie, et le journal le dit.</p>
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
}
