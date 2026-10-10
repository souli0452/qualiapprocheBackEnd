package com.qualiapproche.userservice.service;

import com.qualiapproche.common.courriel.FileDeCourriels;
import com.qualiapproche.common.courriel.StatutCourriel;
import com.qualiapproche.userservice.courriel.CourrielSortant;
import com.qualiapproche.userservice.courriel.CourrielSortantRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import com.qualiapproche.common.config.MailConfig;
import com.qualiapproche.common.config.ThymeleafConfig;
import com.qualiapproche.userservice.service.impl.SendMailServiceImpl;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.mail.internet.MimeMultipart;

/**
 * Courriels de compte : vérification, réinitialisation, mot de passe temporaire.
 *
 * <p>Ces envois ont cessé de fonctionner sans qu'aucune erreur parlante n'apparaisse. Deux causes s'y
 * cumulaient : aucun expéditeur n'était posé — un relais authentifié refuse un message dont
 * l'expéditeur est absent ou étranger à la boîte qui s'authentifie — et le corps était rendu par un
 * moteur dont la résolution des gabarits n'était pas vérifiée.</p>
 *
 * <p>D'où des tests qui vont jusqu'au message expédié, avec le <b>vrai</b> moteur de gabarits et les
 * <b>vrais</b> fichiers du projet : un gabarit inventé pour le test ne prouverait rien de ce
 * défaut-là. Seul le serveur SMTP est simulé — c'est ce qu'on lui remet qui est examiné.</p>
 */
class CourrielsDeCompteTest {

    private static final String BOITE_AUTHENTIFIEE = "noreply@qualisira.com";

    private JavaMailSender mailSender;
    private SendMailServiceImpl service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(i -> new MimeMessage((jakarta.mail.Session) null));

        MailConfig mailConfig = new MailConfig();
        mailConfig.setUsername(BOITE_AUTHENTIFIEE);
        TemplateEngine moteur = ThymeleafConfig.getTemplateEngine();

        derniersCourriels.clear();
        file = fileEnMemoire(mailSender);
        service = new SendMailServiceImpl(moteur, file);
    }

    private FileDeCourriels<CourrielSortant> file;
    private static final Map<UUID, CourrielSortant> derniersCourriels = new HashMap<>();

    /** La vraie file, sur un dépôt en mémoire : le courriel est déposé, puis remis par la tournée. */
    @SuppressWarnings("unchecked")
    private static FileDeCourriels<CourrielSortant> fileEnMemoire(JavaMailSender serveur) {
        Map<UUID, CourrielSortant> base = derniersCourriels;
        CourrielSortantRepository depot = mock(CourrielSortantRepository.class);
        when(depot.save(any(CourrielSortant.class))).thenAnswer(i -> {
            CourrielSortant c = i.getArgument(0);
            base.put(c.getId(), c);
            return c;
        });
        when(depot.findById(any())).thenAnswer(i -> Optional.ofNullable(base.get((UUID) i.getArgument(0))));
        when(depot.echus(any(), anyInt())).thenAnswer(i -> base.values().stream()
                .filter(c -> c.getStatut() == StatutCourriel.A_ENVOYER).map(CourrielSortant::getId).toList());
        when(depot.revendiquer(any(), any(), any(), any(), any())).thenAnswer(i -> {
            CourrielSortant c = base.get((UUID) i.getArgument(0));
            c.setStatut(StatutCourriel.EN_COURS);
            c.setTentatives(c.getTentatives() + 1);
            return 1;
        });
        ObjectProvider<JavaMailSender> messagerie = mock(ObjectProvider.class);
        when(messagerie.getIfAvailable()).thenReturn(serveur);
        return new FileDeCourriels<>(depot, CourrielSortant::new, messagerie, BOITE_AUTHENTIFIEE,
                mock(PlatformTransactionManager.class));
    }

    /** Message réellement remis au serveur. */
    private MimeMessage expedie() {
        file.tournee(50);
        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(aMessage.capture());
        return aMessage.getValue();
    }

    private String corps(MimeMessage message) throws Exception {
        String html = extraireHtml(message.getContent());
        assertThat(html).as("aucune partie HTML dans le message").isNotNull();
        return html;
    }

    private String extraireHtml(Object contenu) throws Exception {
        if (contenu instanceof String texte) {
            return texte;
        }
        if (contenu instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String trouve = extraireHtml(multipart.getBodyPart(i).getContent());
                if (trouve != null) {
                    return trouve;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ création de compte

    @Test
    @DisplayName("La vérification de compte part, avec expéditeur, destinataire et objet")
    void verificationDeCompte_expediee() throws Exception {
        service.sendVerificationEmail("claire@exemple.fr", "Claire", "Martin",
                "MotDePasse1!", "https://qualisira.com/verifier?jeton=abc");

        MimeMessage message = expedie();
        // Sans expéditeur, le relais répond « sender not allowed » et personne ne reçoit rien.
        assertThat(message.getFrom()).isNotNull();
        assertThat(message.getFrom()[0].toString()).contains(BOITE_AUTHENTIFIEE);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
                .contains("claire@exemple.fr");
        assertThat(message.getSubject()).isEqualTo("Vérification de votre compte");
    }

    @Test
    @DisplayName("Le corps de la vérification est rendu : mot de passe temporaire et lien y figurent")
    void verificationDeCompte_corpsRendu() throws Exception {
        service.sendVerificationEmail("claire@exemple.fr", "Claire", "Martin",
                "MotDePasse1!", "https://qualisira.com/verifier?jeton=abc");

        String corps = corps(expedie());
        // Un gabarit non résolu, ou résolu mais non substitué, laisserait le destinataire sans le
        // mot de passe qui lui permet d'entrer.
        assertThat(corps).contains("MotDePasse1!");
        assertThat(corps).contains("https://qualisira.com/verifier?jeton=abc");
        assertThat(corps).doesNotContain("th:text");
    }

    @Test
    @DisplayName("La demande de réinitialisation part avec son lien")
    void reinitialisation_expediee() throws Exception {
        service.sendReinitializePasswordEmail("claire@exemple.fr",
                "https://qualisira.com/reinitialiser?jeton=xyz");

        MimeMessage message = expedie();
        assertThat(message.getFrom()[0].toString()).contains(BOITE_AUTHENTIFIEE);
        assertThat(message.getSubject()).isEqualTo("Réinitialisation de votre mot de passe");
        assertThat(corps(message)).contains("https://qualisira.com/reinitialiser?jeton=xyz");
    }

    @Test
    @DisplayName("Le mot de passe temporaire part avec sa valeur et son adresse d'accès")
    void motDePasseTemporaire_expedie() throws Exception {
        service.sendResetPasswordEmail("claire@exemple.fr", "Provisoire9!",
                "https://qualisira.com/connexion");

        MimeMessage message = expedie();
        assertThat(message.getFrom()[0].toString()).contains(BOITE_AUTHENTIFIEE);
        assertThat(corps(message)).contains("Provisoire9!");
        assertThat(corps(message)).contains("https://qualisira.com/connexion");
    }

    @Test
    @DisplayName("Un courriel libre part aussi avec un expéditeur")
    void courrielLibre_avecExpediteur() throws Exception {
        service.sendMail("claire@exemple.fr", "Information", "<p>Bonjour</p>", true);

        MimeMessage message = expedie();
        assertThat(message.getFrom()[0].toString()).contains(BOITE_AUTHENTIFIEE);
        assertThat(corps(message)).contains("<p>Bonjour</p>");
    }

    // ------------------------------------------------------------------ échecs

    @Test
    @DisplayName("Un refus du serveur ne perd pas le courriel : il reste en file, avec sa cause, pour être retenté")
    void refusDuServeur_courrielConserve() {
        doThrow(new MailSendException("authentification refusée"))
                .when(mailSender).send(any(MimeMessage.class));

        // Le geste de l'utilisateur aboutit : le courriel est déposé, la panne ne le concerne pas.
        service.sendVerificationEmail("claire@exemple.fr", "Claire", "Martin", "MotDePasse1!",
                "https://qualisira.com/verifier");
        file.tournee(50);

        // Le serveur a bien été sollicité, et son refus n'a rien effacé : le courriel attend sa
        // prochaine tentative au lieu de disparaître dans un journal.
        verify(mailSender).send(any(MimeMessage.class));
        org.assertj.core.api.Assertions.assertThat(derniersCourriels.values()).singleElement()
                .satisfies(c -> {
                    org.assertj.core.api.Assertions.assertThat(c.getStatut()).isEqualTo(StatutCourriel.A_ENVOYER);
                    org.assertj.core.api.Assertions.assertThat(c.getDerniereErreur()).contains("authentification refusée");
                });
    }
}
