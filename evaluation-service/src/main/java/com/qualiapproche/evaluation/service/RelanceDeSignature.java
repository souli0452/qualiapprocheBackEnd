package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.messagerie.Courriels;
import com.qualiapproche.evaluation.client.AnnuaireClient;
import com.qualiapproche.evaluation.entities.Signature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Le courriel qui rappelle à un signataire que le rapport attend son visa.
 *
 * <p>Il n'existait aucune relance : un signataire absent retenait la clôture sans que personne ne
 * puisse le lui rappeler depuis l'application (recette du 17 oct. 2026, observation 26). Le
 * courriel passe par la boîte d'envoi du service, comme les autres.</p>
 */
@Slf4j
@Component
public class RelanceDeSignature {

    static final String GABARIT = "courriel/relanceSignature";

    private final AnnuaireClient annuaire;
    private final Courriels courriels;
    private final TemplateEngine moteur;
    private final String lienAudit;

    public RelanceDeSignature(AnnuaireClient annuaire, Courriels courriels, TemplateEngine moteur,
                              @Value("${qualisira.audit.lien-audit:}") String lienAudit) {
        this.annuaire = annuaire;
        this.courriels = courriels;
        this.moteur = moteur;
        this.lienAudit = lienAudit;
    }

    /** Dépose la relance ; refuse, en le disant, si le signataire n'a pas d'adresse connue. */
    public void relancer(Signature visa, String relancePar) {
        Map<String, Object> personne;
        try {
            personne = annuaire.utilisateur(visa.getSignataireId());
        } catch (Exception e) {
            log.warn("Annuaire injoignable pour relancer le signataire {} : {}", visa.getSignataireId(), e.getMessage());
            throw new BusinessException("L'annuaire ne répond pas : la relance n'a pas pu partir.", HttpStatus.BAD_GATEWAY);
        }
        String adresse = texte(personne == null ? null : personne.get("email"));
        if (adresse == null) {
            throw new BusinessException("Ce signataire n'a pas d'adresse électronique dans l'annuaire :"
                    + " la relance ne peut pas partir.", HttpStatus.CONFLICT);
        }
        String nom = texte(personne.get("fullName"));
        if (nom == null) {
            String compose = ((texte(personne.get("firstName")) == null ? "" : texte(personne.get("firstName")))
                    + " " + (texte(personne.get("lastName")) == null ? "" : texte(personne.get("lastName")))).trim();
            nom = compose.isEmpty() ? "Madame, Monsieur" : compose;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("nomComplet", nom);
        variables.put("reference", visa.getAudit().getReference());
        variables.put("referenceRapport", visa.getAudit().getReferenceRapport());
        variables.put("role", visa.getRole() == null ? null : visa.getRole().getLibelle());
        variables.put("relancePar", relancePar);
        variables.put("lien", lienAudit.isBlank() ? null : lienAudit.replace("{id}", visa.getAudit().getId().toString()));
        Context contexte = new Context();
        contexte.setVariables(variables);
        courriels.envoyer(adresse, null, "Audit " + visa.getAudit().getReference() + " — rapport en attente de votre signature",
                moteur.process(GABARIT, contexte), "relance-signature");
    }

    private static String texte(Object valeur) {
        return valeur == null || valeur.toString().isBlank() ? null : valeur.toString().strip();
    }
}
