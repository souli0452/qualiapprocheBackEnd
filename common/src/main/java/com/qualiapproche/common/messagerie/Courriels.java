package com.qualiapproche.common.messagerie;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Envoyer un courriel, depuis n'importe quel service : il est déposé dans la boîte d'envoi du
 * service, publié sur {@link AdressesMessagerie#COURRIELS}, et workflow-service le remet par SMTP.
 *
 * <p>Seul chemin d'envoi de l'application. Il y en avait six — chaque service ouvrait sa propre
 * session SMTP, avec ses propres réglages et sa propre façon de perdre un courriel.</p>
 */
@Slf4j
public class Courriels {

    private final BoiteDEnvoi<?> boite;

    public Courriels(BoiteDEnvoi<?> boite) {
        this.boite = boite;
    }

    /** Dépose un courriel en texte brut : échappé, ses lignes préservées. */
    public UUID envoyerTexte(String destinataire, String sujet, String texte, String origine) {
        String corps = org.springframework.web.util.HtmlUtils.htmlEscape(texte == null ? "" : texte).replace("\n", "<br>");
        return envoyer(destinataire, null, sujet, corps, origine);
    }

    /**
     * Dépose un courriel déjà rendu. Dans une transaction, il ne partira que si elle est validée.
     *
     * @return l'identifiant du message, ou {@code null} s'il n'y avait aucun destinataire
     */
    public UUID envoyer(String destinataire, String copie, String sujet, String corpsHtml, String origine) {
        return deposer(AdressesMessagerie.TYPE_COURRIEL, destinataire, copie, sujet, corpsHtml, origine);
    }

    /**
     * Dépose un courriel qui porte un secret — mot de passe temporaire, lien de vérification ou de
     * réinitialisation. Il part comme les autres, mais son contenu est effacé de la boîte d'envoi
     * dès sa publication : le jeton n'est gardé qu'haché en base, son lien ne doit pas y rester en
     * clair.
     */
    public UUID envoyerConfidentiel(String destinataire, String sujet, String corpsHtml, String origine) {
        return deposer(AdressesMessagerie.TYPE_COURRIEL_CONFIDENTIEL, destinataire, null, sujet, corpsHtml, origine);
    }

    private UUID deposer(String type, String destinataire, String copie, String sujet, String corpsHtml,
                         String origine) {
        if (destinataire == null || destinataire.isBlank()) {
            log.warn("Courriel « {} » ({}) non envoyé : aucun destinataire.", sujet, origine);
            return null;
        }
        return boite.deposer(AdressesMessagerie.COURRIELS, type,
                CourrielAEnvoyer.builder()
                        .destinataire(destinataire.trim())
                        .copie(copie == null || copie.isBlank() ? null : copie.trim())
                        .sujet(sujet == null ? "" : sujet)
                        .corps(corpsHtml == null ? "" : corpsHtml)
                        .origine(origine)
                        .build());
    }
}
