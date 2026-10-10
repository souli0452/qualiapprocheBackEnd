package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.config.EmailTemplateEngineConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rend les courriels d'étape : gabarit Thymeleaf enregistré en base, variables, et pied de
 * courriel de l'organisation.
 *
 * <p>N'envoie plus rien. L'envoi passe par la boîte d'envoi du service ({@code Courriels}), puis par
 * la file {@code qualisira.courriels.envoi}, que {@code RemiseDesCourriels} consomme — le seul
 * point d'envoi SMTP de l'application. Ce service s'appelait {@code SmtpEmailService} et ouvrait
 * sa propre session SMTP, une parmi les six que comptait l'application.</p>
 */
@Service
@Slf4j
public class RenduDesCourriels {

    private final TemplateEngine moteurGabarit;
    private final PiedDeCourriel piedDeCourriel;

    public RenduDesCourriels(@Qualifier(EmailTemplateEngineConfig.MOTEUR_GABARIT_EMAIL) TemplateEngine moteurGabarit,
                             PiedDeCourriel piedDeCourriel) {
        this.moteurGabarit = moteurGabarit;
        this.piedDeCourriel = piedDeCourriel;
    }

    /**
     * Le corps final d'un courriel : le gabarit rendu avec ses variables, suivi de la signature de
     * l'organisation.
     *
     * <p>Le rendu se faisait par remplacement littéral de marqueurs {@code {{clé}}} alors que les
     * gabarits livrés sont tous écrits en Thymeleaf : aucune variable n'était substituée. Thymeleaf
     * apporte en outre l'échappement HTML des valeurs. La signature vient des réglages : l'écrire
     * dans chaque gabarit l'aurait figée en quinze exemplaires.</p>
     *
     * @param htmlTemplate corps du gabarit, tel qu'enregistré en base
     * @param variables    valeurs exposées au gabarit ; une variable absente rend une chaîne vide
     */
    public String corps(String htmlTemplate, Map<String, String> variables) {
        return piedDeCourriel.ajouterAu(rendre(htmlTemplate, variables));
    }

    /**
     * Rend le gabarit avec les variables fournies.
     *
     * <p>Un gabarit syntaxiquement invalide — il est éditable par API, donc faillible — n'empêche
     * pas l'envoi : un message imparfait vaut mieux qu'un responsable jamais prévenu. Mais ce qui
     * part alors est un {@linkplain #corpsDeSecours message neutre}, et non plus le gabarit brut :
     * celui-ci exposait au destinataire les expressions et le balisage internes du modèle, et un
     * gabarit délibérément rendu invalide devenait un moyen d'envoyer un HTML arbitraire, sans
     * l'échappement que le rendu garantit aux valeurs.</p>
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
            log.error("Gabarit d'e-mail illisible, un message neutre part à sa place : {}", e.getMessage());
            return corpsDeSecours(variables);
        }
    }

    /**
     * Message de remplacement quand le gabarit ne se rend pas.
     *
     * <p>Il dit l'essentiel — qui est prévenu, de quel dossier, où le trouver — et rien d'autre.
     * Chaque valeur est échappée : elles viennent de saisies (référence, nom), et ce message est
     * précisément celui qui se passe du moteur qui les échappait. Le lien n'est repris que s'il
     * est en {@code http(s)} : c'est {@code LienVersLeDossier} qui le forme, mais un {@code href}
     * ne doit jamais pouvoir porter un {@code javascript:}.</p>
     */
    static String corpsDeSecours(Map<String, String> variables) {
        Map<String, String> valeurs = variables == null ? Map.of() : variables;
        StringBuilder corps = new StringBuilder();
        String nom = valeurs.get("fullName");
        corps.append("<p>Bonjour").append(nom == null || nom.isBlank() ? "" : " " + HtmlUtils.htmlEscape(nom))
                .append(",</p>");
        String reference = valeurs.get("reference");
        String etape = valeurs.get("etape");
        corps.append("<p>Un dossier")
                .append(reference == null || reference.isBlank() ? "" : " (" + HtmlUtils.htmlEscape(reference) + ")")
                .append(" requiert votre attention")
                .append(etape == null || etape.isBlank() ? "" : " à l'étape « " + HtmlUtils.htmlEscape(etape) + " »")
                .append(".</p>");
        String lien = valeurs.get("link");
        if (lien != null && (lien.startsWith("https://") || lien.startsWith("http://"))) {
            corps.append("<p><a href=\"").append(HtmlUtils.htmlEscape(lien)).append("\">Ouvrir le dossier</a></p>");
        }
        return corps.toString();
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
