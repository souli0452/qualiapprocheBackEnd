package com.qualiapproche.userservice.service;

import com.qualiapproche.common.config.ThymeleafConfig;
import com.qualiapproche.common.messagerie.Courriels;
import com.qualiapproche.userservice.service.impl.SendMailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Courriels de compte : vérification, réinitialisation, mot de passe temporaire.
 *
 * <p>Ces envois ont cessé un temps de fonctionner sans erreur parlante, entre autres parce que le
 * corps était rendu par un moteur dont la résolution des gabarits n'était pas vérifiée. D'où des
 * tests qui rendent avec le <b>vrai</b> moteur et les <b>vrais</b> gabarits du projet, et examinent
 * le courriel tel qu'il est déposé dans la boîte d'envoi.</p>
 *
 * <p>L'expéditeur et la remise au serveur de messagerie relèvent désormais du consommateur des
 * courriels (workflow-service), qui les teste de son côté.</p>
 */
class CourrielsDeCompteTest {

    private Courriels courriels;
    private SendMailServiceImpl service;

    @BeforeEach
    void setUp() {
        courriels = mock(Courriels.class);
        TemplateEngine moteur = ThymeleafConfig.getTemplateEngine();
        service = new SendMailServiceImpl(moteur, courriels);
    }

    /**
     * Le corps d'un courriel confidentiel déposé pour ce destinataire et cet objet : il porte un
     * mot de passe ou un lien à jeton, et ne doit pas rester dans la boîte d'envoi une fois parti.
     */
    private String corpsConfidentiel(String destinataire, String sujet) {
        ArgumentCaptor<String> corps = ArgumentCaptor.forClass(String.class);
        verify(courriels).envoyerConfidentiel(eq(destinataire), eq(sujet), corps.capture(), anyString());
        return corps.getValue();
    }

    /** Le corps déposé pour ce destinataire et cet objet. */
    private String corpsDepose(String destinataire, String sujet) {
        ArgumentCaptor<String> corps = ArgumentCaptor.forClass(String.class);
        verify(courriels).envoyer(eq(destinataire), isNull(), eq(sujet), corps.capture(), anyString());
        return corps.getValue();
    }

    @Test
    @DisplayName("La vérification de compte est rendue : mot de passe temporaire et lien y figurent")
    void verificationDeCompte() {
        service.sendVerificationEmail("claire@exemple.fr", "Claire", "Martin",
                "MotDePasse1!", "https://qualisira.com/verifier?jeton=abc");

        String corps = corpsConfidentiel("claire@exemple.fr", "Vérification de votre compte");
        // Un gabarit non résolu, ou résolu mais non substitué, laisserait le destinataire sans le
        // mot de passe qui lui permet d'entrer.
        assertThat(corps).contains("MotDePasse1!", "https://qualisira.com/verifier?jeton=abc");
        assertThat(corps).doesNotContain("th:text");
    }

    @Test
    @DisplayName("La demande de réinitialisation porte son lien")
    void reinitialisation() {
        service.sendReinitializePasswordEmail("claire@exemple.fr", "https://qualisira.com/reinitialiser?jeton=xyz");

        assertThat(corpsConfidentiel("claire@exemple.fr", "Réinitialisation de votre mot de passe"))
                .contains("https://qualisira.com/reinitialiser?jeton=xyz");
    }

    @Test
    @DisplayName("Le mot de passe temporaire et le lien de connexion figurent dans le courriel")
    void motDePasseTemporaire() {
        service.sendResetPasswordEmail("claire@exemple.fr", "Provisoire9!", "https://qualisira.com/connexion");

        assertThat(corpsConfidentiel("claire@exemple.fr", "Votre nouveau mot de passe temporaire"))
                .contains("Provisoire9!", "https://qualisira.com/connexion");
    }

    @Test
    @DisplayName("Un message HTML part tel quel ; un texte brut est échappé, ses lignes préservées")
    void courrielLibre() {
        service.sendMail("claire@exemple.fr", "Information", "<p>Bonjour</p>", true);
        assertThat(corpsDepose("claire@exemple.fr", "Information")).isEqualTo("<p>Bonjour</p>");

        Courriels autres = mock(Courriels.class);
        new SendMailServiceImpl(ThymeleafConfig.getTemplateEngine(), autres)
                .sendMail("claire@exemple.fr", "Texte", "a < b\nligne 2", false);
        verify(autres).envoyer(eq("claire@exemple.fr"), isNull(), eq("Texte"), eq("a &lt; b<br>ligne 2"), anyString());
    }
}
