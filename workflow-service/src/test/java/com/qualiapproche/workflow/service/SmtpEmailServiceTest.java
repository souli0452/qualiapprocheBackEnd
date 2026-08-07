package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.config.EmailTemplateEngineConfig;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rendu des courriels.
 *
 * <p>Le corps était traité par un remplacement littéral de {@code {{clé}}} alors que les gabarits
 * livrés sont écrits en Thymeleaf : aucune variable n'était substituée, et les messages partaient
 * avec leurs attributs {@code th:} bruts. Ces tests s'appuient sur les gabarits <b>réels</b> du
 * projet — un gabarit inventé pour le test ne prouverait rien de ce défaut.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmtpEmailServiceTest {

    @Mock private JavaMailSender mailSender;

    private SmtpEmailService service;

    @BeforeEach
    void setUp() {
        // Pied de page neutre : le référentiel est injoignable dans un test unitaire, et le pied
        // s'abstient alors — ce qui laisse ces vérifications porter sur le seul gabarit.
        ReglagesOrganisation reglagesInjoignables = new ReglagesOrganisation(() -> {
            throw new IllegalStateException("référentiel indisponible");
        });
        ReflectionTestUtils.setField(reglagesInjoignables, "retentionSecondes", 600L);
        PiedDeCourriel aPied = new PiedDeCourriel(reglagesInjoignables);
        service = new SmtpEmailService(mailSender, new EmailTemplateEngineConfig().moteurGabaritEmail(), aPied);
        ReflectionTestUtils.setField(service, "fromEmail", "qualite@exemple.fr");
        when(mailSender.createMimeMessage()).thenAnswer(i ->
                new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null));
    }

    /** Gabarit réellement livré, chargé depuis le classpath comme le fait l'initialiseur. */
    private String gabaritLivre(String code) throws IOException {
        try (var flux = new ClassPathResource("templates/" + code + ".html").getInputStream()) {
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Corps HTML réellement expédié.
     *
     * <p>Le message est multipart : lire {@code getContent()} directement rendrait la
     * représentation de l'enveloppe, sur laquelle toute assertion de non-présence passerait à
     * vide — y compris contre un rendu défaillant.</p>
     */
    private String corpsEnvoye() throws Exception {
        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(aMessage.capture());
        String aCorps = extraireHtml(aMessage.getValue().getContent());
        assertThat(aCorps).as("aucune partie HTML trouvée dans le message").isNotNull();
        return aCorps;
    }

    private String extraireHtml(Object contenu) throws Exception {
        if (contenu instanceof String texte) {
            return texte;
        }
        if (contenu instanceof jakarta.mail.internet.MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String aTrouve = extraireHtml(multipart.getBodyPart(i).getContent());
                if (aTrouve != null) {
                    return aTrouve;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ gabarits réels

    @Test
    @DisplayName("Un gabarit livré voit ses variables réellement substituées")
    void gabaritLivre_variablesSubstituees() throws Exception {
        String aGabarit = gabaritLivre("validationRq");

        service.sendEmail("claire@exemple.fr", "Validation attendue", aGabarit,
                Map.of("fullName", "Claire Martin",
                        "numeroNc", "NC-2026-014",
                        "link", "https://qualite.exemple.fr/nc/42"));

        String aCorps = corpsEnvoye();
        assertThat(aCorps).contains("Claire Martin");
        assertThat(aCorps).contains("NC-2026-014");
        assertThat(aCorps).contains("https://qualite.exemple.fr/nc/42");
    }

    @Test
    @DisplayName("Aucune syntaxe Thymeleaf ne subsiste dans le message envoyé")
    void gabaritLivre_aucuneSyntaxeThymeleafResiduelle() throws Exception {
        String aGabarit = gabaritLivre("validationNonConformite");

        // Le gabarit brut porte « [[${fullName}]] » et « th:href » : c'est exactement ce que le
        // destinataire recevait, l'ancien rendu ne touchant qu'à d'hypothétiques {{clé}}.
        assertThat(aGabarit).contains("[[${").contains("th:href");

        service.sendEmail("claire@exemple.fr", "Validation requise", aGabarit,
                Map.of("fullName", "Claire Martin", "numeroNc", "NC-1", "link", "https://exemple.fr/x"));

        assertThat(corpsEnvoye())
                .as("le message partait avec ses expressions Thymeleaf non interprétées")
                .doesNotContain("[[${")
                .doesNotContain("th:href")
                .doesNotContain("xmlns:th");
    }

    @Test
    @DisplayName("Une variable non fournie ne laisse pas de marqueur visible")
    void variableAbsente_aucunMarqueurVisible() throws Exception {
        String aGabarit = gabaritLivre("emailPlanAction");

        service.sendEmail("claire@exemple.fr", "Plan d'action", aGabarit,
                Map.of("fullName", "Claire Martin"));

        assertThat(corpsEnvoye()).doesNotContain("${").doesNotContain("{{");
    }

    // ------------------------------------------------------------------ robustesse et sécurité

    @Test
    @DisplayName("Les valeurs sont échappées : un commentaire balisé ne s'injecte pas dans le message")
    void valeurs_echappees() throws Exception {
        String aGabarit = "<p th:text=\"${observation}\">commentaire</p>";

        service.sendEmail("claire@exemple.fr", "Objet", aGabarit,
                Map.of("observation", "<script>alert('xss')</script>"));

        String aCorps = corpsEnvoye();
        assertThat(aCorps)
                .as("le remplacement littéral recopiait la valeur telle quelle dans le HTML")
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("Un gabarit invalide n'empêche pas l'envoi")
    void gabaritInvalide_envoiQuandMeme() throws Exception {
        service.sendEmail("claire@exemple.fr", "Objet",
                "<p th:text=\"${ceci n'est pas une expression}\">x</p>", Map.of());

        // Un gabarit est éditable par API, donc faillible : un responsable doit être prévenu
        // même par un message imparfait.
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    @DisplayName("Un corps vide ou nul n'entraîne pas d'erreur")
    void corpsVide_pasDerreur() {
        service.sendEmail("claire@exemple.fr", "Objet", null, Map.of());
        service.sendEmail("claire@exemple.fr", "Objet", "  ", null);

        verify(mailSender, org.mockito.Mockito.times(2))
                .send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    @DisplayName("Le destinataire et l'expéditeur sont bien posés")
    void enteteDuMessage() throws Exception {
        service.sendEmail("claire@exemple.fr", "Un dossier vous attend",
                "<p th:text=\"${fullName}\">x</p>", Map.of("fullName", "Claire Martin"));

        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(aMessage.capture());

        assertThat(aMessage.getValue().getAllRecipients()[0].toString()).isEqualTo("claire@exemple.fr");
        assertThat(aMessage.getValue().getFrom()[0].toString()).isEqualTo("qualite@exemple.fr");
        assertThat(aMessage.getValue().getSubject()).isEqualTo("Un dossier vous attend");
    }

    // ------------------------------------------------------------------ échecs d'envoi

    @Test
    @DisplayName("Un refus du serveur SMTP remonte, au lieu d'être avalé en une ligne de journal")
    void refusDuServeur_remonte() {
        // L'envoi attrapait l'échec et rendait la main normalement : le registre des notifications
        // marquait alors la notification « remise » alors qu'aucun courriel n'était parti, et ne la
        // rejouait jamais. Un mot de passe expiré se traduisait par un silence complet.
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailAuthenticationException("535 auth failed"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(jakarta.mail.internet.MimeMessage.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.sendEmail("sam@exemple.fr", "Objet", "<p>Corps</p>", java.util.Map.of()))
                .isInstanceOf(org.springframework.mail.MailException.class);
    }

    @Test
    @DisplayName("Une adresse de destinataire inexploitable remonte aussi, sans être rejouée en boucle")
    void adresseInexploitable_remonte() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.sendEmail("pas une adresse", "Objet", "<p>Corps</p>", java.util.Map.of()))
                .isInstanceOf(org.springframework.mail.MailException.class);
    }

    // ------------------------------------------------------------------ copie

    @Test
    @DisplayName("La copie demandée figure bien en Cc du message expédié")
    void copie_posEeEnCc() throws Exception {
        service.sendEmail("claire@exemple.fr", "Non-conformité", "<p>Corps</p>", Map.of(),
                "rq@exemple.fr");

        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(aMessage.capture());
        assertThat(aMessage.getValue().getRecipients(jakarta.mail.Message.RecipientType.CC))
                .as("sans en-tête Cc, le responsable qualité ne recevrait rien")
                .isNotNull()
                .anyMatch(adresse -> adresse.toString().contains("rq@exemple.fr"));
    }

    @Test
    @DisplayName("Aucune copie demandée : pas d'en-tête Cc vide, que certains relais rejettent")
    void aucuneCopie_pasDEnTeteVide() throws Exception {
        service.sendEmail("claire@exemple.fr", "Document", "<p>Corps</p>", Map.of(), "   ");

        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(aMessage.capture());
        assertThat(aMessage.getValue().getRecipients(jakarta.mail.Message.RecipientType.CC)).isNull();
    }
}
