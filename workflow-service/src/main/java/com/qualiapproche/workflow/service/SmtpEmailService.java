package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.config.EmailTemplateEngineConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
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
    private final PiedDeCourriel piedDeCourriel;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Qualifier(EmailTemplateEngineConfig.MOTEUR_GABARIT_EMAIL) TemplateEngine moteurGabarit,
                            PiedDeCourriel piedDeCourriel) {
        this.mailSender = mailSender;
        this.moteurGabarit = moteurGabarit;
        this.piedDeCourriel = piedDeCourriel;
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
     * @throws org.springframework.mail.MailException si le serveur refuse ou n'est pas joignable —
     *         volontairement propagée : le registre des notifications l'inscrit et programme une
     *         reprise. Rien ici ne doit faire croire à un envoi qui n'a pas eu lieu.
     */
    public void sendEmail(String to, String subject, String htmlTemplate, Map<String, String> variables) {
        sendEmail(to, subject, htmlTemplate, variables, null);
    }

    /**
     * Même envoi, avec une adresse en copie.
     *
     * @param copieA adresse à mettre en copie, ou {@code null} — le responsable qualité pour ce qui
     *               sort d'une non-conformité, personne pour le reste. Une adresse vide est ignorée
     *               plutôt que posée : un en-tête {@code Cc} vide fait rejeter le message par
     *               certains relais.
     */
    public void sendEmail(String to, String subject, String htmlTemplate, Map<String, String> variables,
                          String copieA) {
        // Signature de l'organisation, tirée de ses réglages : les gabarits n'en portaient aucune,
        // et un destinataire recevait une demande de validation sans savoir d'où elle venait ni à
        // qui s'adresser. L'écrire dans chaque gabarit l'aurait figée en quinze exemplaires.
        String body = piedDeCourriel.ajouterAu(rendre(htmlTemplate, variables));

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            if (copieA != null && !copieA.isBlank()) {
                helper.setCc(copieA.trim());
            }
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML
        } catch (MessagingException e) {
            // Message impossible à composer — adresse malformée, encodage refusé. Inutile de
            // rejouer : la cause ne changera pas d'elle-même. L'exception remonte quand même, pour
            // que le registre l'inscrive au lieu de laisser croire à une remise.
            throw new MailPreparationException(
                    "Courriel à destination de « " + to + " » impossible à composer : " + e.getMessage(), e);
        }

        // `send` lève des MailException — authentification refusée, hôte injoignable, destinataire
        // rejeté. Elles ne sont pas rattrapées ici : c'est le registre des notifications qui les
        // inscrit et programme la reprise. Les avaler ici marquait la notification « remise » alors
        // que rien n'était parti : aucune reprise, aucune trace exploitable, et un journal qui
        // n'annonçait qu'un envoi « réussi » pour les autres.
        mailSender.send(message);
        log.info("Courriel remis au serveur SMTP pour {}", to);
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
