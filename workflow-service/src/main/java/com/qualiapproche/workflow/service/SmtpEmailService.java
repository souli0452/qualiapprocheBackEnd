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
     * @throws MailException si le serveur refuse ou n'est pas joignable —
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
            return moteurGabarit.process(accoladesVersThymeleaf(htmlTemplate, variables), contexte);
        } catch (Exception e) {
            log.error("Gabarit d'e-mail illisible, le corps est envoyé sans substitution : {}", e.getMessage());
            return htmlTemplate;
        }
    }

    /**
     * Traduit la syntaxe {@code {variable}} de l'éditeur vers Thymeleaf, avant le rendu.
     *
     * <p>Les gabarits livrés sont écrits en Thymeleaf ({@code [[${numeroNc}]]}), mais cette syntaxe
     * n'est pas celle qu'un rédacteur de modèle devine : {@code {numeroNc}} l'est. Plutôt qu'un
     * second moteur de substitution — il y en a déjà eu deux, et c'est ce qui avait cassé les
     * envois — les accolades sont réécrites en inline Thymeleaf et le rendu reste unique :
     * même échappement HTML, mêmes règles, gabarits existants intacts.</p>
     *
     * <p>Seules les variables <b>fournies</b> sont réécrites : les accolades du CSS des gabarits
     * ({@code p '{' margin: 0 '}'}) et un {@code {mot}} quelconque ne sont pas touchés.</p>
     */
    private String accoladesVersThymeleaf(String gabarit, Map<String, String> variables) {
        if (variables == null || variables.isEmpty() || gabarit.indexOf('{') < 0) {
            return gabarit;
        }
        String resultat = gabarit;
        for (String nom : variables.keySet()) {
            // Pas d'accolade précédée de « $ » : « [[${fullName}]] » contient « {fullName} », et le
            // réécrire donnerait un gabarit illisible — les modèles livrés sont écrits ainsi.
            resultat = resultat.replaceAll("(?<!\\$)\\{" + java.util.regex.Pattern.quote(nom) + "\\}",
                    java.util.regex.Matcher.quoteReplacement("[[${" + nom + "}]]"));
        }
        return resultat;
    }

    /**
     * Objet du courriel, avec la même syntaxe {@code {variable}} que le corps.
     *
     * <p>L'objet est du texte simple, hors de portée du moteur HTML : la substitution est directe.
     * Une variable absente ou vide s'efface, et le séparateur qui la précédait avec elle —
     * « Validation attendue – NC {numeroNc} » sans référence redevient « Validation attendue »,
     * pas « Validation attendue – NC  ». Les retours à la ligne des valeurs sont retirés : un
     * en-tête ne doit pas pouvoir être scindé par une valeur saisie.</p>
     */
    public static String sujet(String sujet, Map<String, String> variables) {
        if (sujet == null) {
            return "";
        }
        String resultat = sujet;
        if (variables != null) {
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                String valeur = variable.getValue() == null
                        ? "" : variable.getValue().replaceAll("[\r\n]+", " ").trim();
                resultat = resultat.replace("{" + variable.getKey() + "}", valeur);
            }
        }
        // Variables inconnues restantes, puis séparateurs orphelins en fin d'objet.
        resultat = resultat.replaceAll("\\{[a-zA-Z_][a-zA-Z0-9_]*\\}", "");
        return resultat.replaceAll("(\\s*[–—:-])+\\s*$", "").trim();
    }
}
