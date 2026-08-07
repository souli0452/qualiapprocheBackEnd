package com.qualiapproche.common.utils;

import com.qualiapproche.common.config.MailConfig;
import com.qualiapproche.common.config.ThymeleafConfig;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Envoi d'un courriel à partir d'un gabarit de fichier — le chemin des alertes de plan d'action et
 * des imputations de non-conformité.
 *
 * <p>Il remplace {@code MailUtils}, qui ouvrait sa propre session JavaMail à côté de celle du
 * contexte : deux chemins d'envoi par service, deux jeux de réglages, et un expéditeur posé au petit
 * bonheur. Ces envois ne partaient plus, sans erreur parlante.</p>
 *
 * <p>Les gabarits utilisés ici sont ceux du projet, résolus par le moteur du projet : un gabarit
 * inventé pour le test ne prouverait rien de la résolution, qui est précisément ce dont dépend le
 * corps du message.</p>
 */
class CourrielParGabaritTest {

    private static final String BOITE_AUTHENTIFIEE = "noreply@qualisira.com";

    private JavaMailSender mailSender;
    private TemplateEngine moteur;
    private CourrielParGabarit courriel;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(i -> new MimeMessage((jakarta.mail.Session) null));

        MailConfig mailConfig = new MailConfig();
        mailConfig.setUsername(BOITE_AUTHENTIFIEE);
        moteur = ThymeleafConfig.getTemplateEngine();
        courriel = new CourrielParGabarit(mailSender, mailConfig);
    }

    private EmailMessage alerte(String destinataire, String copie) {
        return EmailMessage.builder()
                .subject("Traitement du plan d'action N° ordre 12")
                .toAddress(destinataire)
                .ccAddress(copie)
                .build();
    }

    private Map<String, Object> variables() {
        return Map.of("link", "https://qualisira.com/traitement-action/non-traiter",
                "fullName", "Claire Martin",
                "numeroNc", "NC-2026-014",
                "observation", "2");
    }

    private MimeMessage expedie() {
        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(aMessage.capture());
        return aMessage.getValue();
    }

    private String corps(MimeMessage message) throws Exception {
        Object contenu = message.getContent();
        if (contenu instanceof String texte) {
            return texte;
        }
        jakarta.mail.internet.MimeMultipart multipart = (jakarta.mail.internet.MimeMultipart) contenu;
        return multipart.getBodyPart(0).getContent().toString();
    }

    @Test
    @DisplayName("L'alerte de plan d'action part, avec expéditeur, destinataire et objet")
    void alerte_expediee() throws Exception {
        courriel.envoyer(alerte("claire@exemple.fr", null), variables(),
                Collections.emptyList(), "alertePlanAction", moteur);

        MimeMessage message = expedie();
        // Sans expéditeur, un relais authentifié refuse le message : c'est l'une des deux causes de
        // la panne d'envoi.
        assertThat(message.getFrom()[0].toString()).contains(BOITE_AUTHENTIFIEE);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
                .contains("claire@exemple.fr");
        assertThat(message.getSubject()).isEqualTo("Traitement du plan d'action N° ordre 12");
    }

    @Test
    @DisplayName("Le corps vient du gabarit du projet, variables substituées")
    void corps_renduDepuisLeGabarit() throws Exception {
        courriel.envoyer(alerte("claire@exemple.fr", null), variables(),
                Collections.emptyList(), "alertePlanAction", moteur);

        String corps = corps(expedie());
        assertThat(corps).contains("Claire Martin");
        assertThat(corps).contains("NC-2026-014");
        // Un gabarit non résolu ou non interprété laisserait ses attributs bruts dans le message.
        assertThat(corps).doesNotContain("th:text");
    }

    @Test
    @DisplayName("Le responsable qualité demandé en copie figure en Cc")
    void copie_posee() throws Exception {
        courriel.envoyer(alerte("claire@exemple.fr", "rq@exemple.fr"), variables(),
                Collections.emptyList(), "alertePlanAction", moteur);

        assertThat(expedie().getRecipients(Message.RecipientType.CC)[0].toString())
                .contains("rq@exemple.fr");
    }

    @Test
    @DisplayName("Aucune copie demandée : pas d'en-tête Cc vide, que certains relais rejettent")
    void aucuneCopie_pasDEnTeteVide() throws Exception {
        courriel.envoyer(alerte("claire@exemple.fr", "   "), variables(),
                Collections.emptyList(), "alertePlanAction", moteur);

        assertThat(expedie().getRecipients(Message.RecipientType.CC)).isNull();
    }

    @Test
    @DisplayName("Sans destinataire connu, rien n'est envoyé — et ce n'est pas une erreur")
    void sansDestinataire_aucunEnvoi() {
        courriel.envoyer(alerte(null, null), variables(), Collections.emptyList(),
                "alertePlanAction", moteur);
        courriel.envoyer(alerte("  ", null), variables(), Collections.emptyList(),
                "alertePlanAction", moteur);

        // Certains plans d'action n'ont pas d'adresse de responsable : ce n'est pas une panne.
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Un refus du serveur remonte, au lieu de passer pour un envoi réussi")
    void refusDuServeur_remonte() {
        doThrow(new MailSendException("hôte injoignable"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> courriel.envoyer(alerte("claire@exemple.fr", null), variables(),
                Collections.emptyList(), "alertePlanAction", moteur))
                .isInstanceOf(MailSendException.class);
    }
}
