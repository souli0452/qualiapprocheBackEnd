package com.qualiapproche.workflow.service;

import com.qualiapproche.common.utils.ClesReglages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import org.springframework.web.util.HtmlUtils;

/**
 * Pied de page ajouté au bas des courriels : qui écrit, et comment le joindre.
 *
 * <p>Les gabarits d'étape ne portaient aucune signature : un destinataire recevait une demande de
 * validation sans savoir de quelle organisation elle venait, ni à qui s'adresser s'il n'était pas
 * concerné. Écrire ces mentions dans chaque gabarit les aurait figées en quinze exemplaires, à
 * corriger un par un le jour où le téléphone change.</p>
 *
 * <p>Les valeurs viennent des réglages de l'organisation ({@code CONTACT_EMAIL},
 * {@code CONTACT_TELEPHONE}, {@code LOGO_URL}…), tenus par referentiel-service. Un réglage vide
 * n'apparaît pas ; tous vides, le pied de page ne s'affiche pas du tout — plutôt qu'un cadre vide au
 * bas de chaque message.</p>
 *
 * <p>Les valeurs sont lues par {@link ReglagesOrganisation}, qui les tient en cache : le référentiel
 * injoignable n'empêche pas l'envoi — le message part sans pied de page, ce qui vaut mieux qu'un
 * responsable jamais prévenu.</p>
 */
@Component
@RequiredArgsConstructor
public class PiedDeCourriel {

    private static final String CLE_ORGANISATION = ClesReglages.ORGANISATION_NOM;
    private static final String CLE_COURRIEL = ClesReglages.CONTACT_EMAIL;
    private static final String CLE_TELEPHONE = ClesReglages.CONTACT_TELEPHONE;
    private static final String CLE_ADRESSE = ClesReglages.ADRESSE_POSTALE;
    private static final String CLE_SITE = ClesReglages.SITE_WEB;
    private static final String CLE_LOGO = ClesReglages.LOGO_URL;

    private final ReglagesOrganisation reglagesOrganisation;

    /**
     * Corps du message, suivi du pied de page.
     *
     * @param corps corps rendu du gabarit
     * @return le corps augmenté ; inchangé si aucun réglage n'est renseigné ou joignable
     */
    public String ajouterAu(String corps) {
        String pied = rendre();
        return pied.isEmpty() ? corps : corps + pied;
    }

    private String rendre() {
        Map<String, String> valeurs = reglagesOrganisation.valeurs();
        if (valeurs.isEmpty()) {
            return "";
        }

        StringBuilder pied = new StringBuilder(
                "<div style=\"margin-top:32px;padding-top:16px;border-top:1px solid #e2e8f0;"
                        + "font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#64748b;\">");

        String logo = valeurs.get(CLE_LOGO);
        if (logo != null) {
            // Hauteur bornée : un logo livré en pleine résolution occuperait tout l'écran du
            // destinataire, la plupart des clients de messagerie ignorant les feuilles de style.
            pied.append("<img src=\"").append(echapper(logo))
                    .append("\" alt=\"\" style=\"max-height:48px;margin-bottom:8px;\" /><br/>");
        }

        String organisation = valeurs.get(CLE_ORGANISATION);
        if (organisation != null) {
            pied.append("<strong>").append(echapper(organisation)).append("</strong><br/>");
        }

        String adresse = valeurs.get(CLE_ADRESSE);
        if (adresse != null) {
            pied.append(echapper(adresse).replace("\n", "<br/>")).append("<br/>");
        }

        // Contact sur une seule ligne : trois lignes pour un téléphone, un courriel et un site
        // allongeraient chaque message sans rien apporter.
        StringBuilder contact = new StringBuilder();
        ajouter(contact, valeurs.get(CLE_TELEPHONE), null);
        ajouter(contact, valeurs.get(CLE_COURRIEL), "mailto:");
        ajouter(contact, valeurs.get(CLE_SITE), "");
        if (contact.length() > 0) {
            pied.append(contact);
        }

        return pied.append("</div>").toString();
    }

    private void ajouter(StringBuilder ligne, String valeur, String prefixeLien) {
        if (valeur == null) {
            return;
        }
        if (ligne.length() > 0) {
            ligne.append(" &nbsp;·&nbsp; ");
        }
        if (prefixeLien == null) {
            ligne.append(echapper(valeur));
            return;
        }
        String href = prefixeLien.isEmpty() ? lienAbsolu(valeur) : prefixeLien + valeur;
        ligne.append("<a href=\"").append(echapper(href))
                .append("\" style=\"color:#2563eb;text-decoration:none;\">")
                .append(echapper(valeur)).append("</a>");
    }

    /** Un site saisi sans protocole donnerait un lien relatif, cassé dans un courriel. */
    private String lienAbsolu(String site) {
        return site.startsWith("http://") || site.startsWith("https://") ? site : "https://" + site;
    }

    /** Les valeurs viennent d'une saisie : elles ne doivent pas pouvoir injecter de balisage. */
    private String echapper(String valeur) {
        return HtmlUtils.htmlEscape(valeur);
    }
}
