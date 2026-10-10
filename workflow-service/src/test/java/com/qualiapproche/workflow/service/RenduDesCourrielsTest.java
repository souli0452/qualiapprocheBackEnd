package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.config.EmailTemplateEngineConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rendu des courriels.
 *
 * <p>Le corps était traité par un remplacement littéral de {@code {{clé}}} alors que les gabarits
 * livrés sont écrits en Thymeleaf : aucune variable n'était substituée, et les messages partaient
 * avec leurs attributs {@code th:} bruts. Ces tests s'appuient sur les gabarits <b>réels</b> du
 * projet — un gabarit inventé pour le test ne prouverait rien de ce défaut.</p>
 *
 * <p>Ce service s'appelait {@code SmtpEmailService} et envoyait lui-même. Il ne fait plus que
 * rendre : l'envoi passe par la boîte d'envoi puis par {@code RemiseDesCourriels}, dont les tests
 * reprennent ce qui portait ici sur le message SMTP (expéditeur, copie, refus du serveur).</p>
 */
class RenduDesCourrielsTest {

    private RenduDesCourriels rendu;

    @BeforeEach
    void setUp() {
        // Pied de page neutre : le référentiel est injoignable dans un test unitaire, et le pied
        // s'abstient alors — ce qui laisse ces vérifications porter sur le seul gabarit.
        ReglagesOrganisation reglagesInjoignables = new ReglagesOrganisation(() -> {
            throw new IllegalStateException("référentiel indisponible");
        });
        ReflectionTestUtils.setField(reglagesInjoignables, "retentionSecondes", 600L);
        rendu = new RenduDesCourriels(new EmailTemplateEngineConfig().moteurGabaritEmail(),
                new PiedDeCourriel(reglagesInjoignables));
    }

    /** Gabarit réellement livré, chargé depuis le classpath comme le fait l'initialiseur. */
    private String gabaritLivre(String code) throws IOException {
        try (var flux = new ClassPathResource("templates/" + code + ".html").getInputStream()) {
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ------------------------------------------------------------------ gabarits réels

    @Test
    @DisplayName("Un gabarit livré voit ses variables réellement substituées")
    void gabaritLivre_variablesSubstituees() throws Exception {
        String corps = rendu.corps(gabaritLivre("validationRq"),
                Map.of("fullName", "Claire Martin",
                        "numeroNc", "NC-2026-014",
                        "link", "https://qualite.exemple.fr/nc/42"));

        assertThat(corps).contains("Claire Martin");
        assertThat(corps).contains("NC-2026-014");
        assertThat(corps).contains("https://qualite.exemple.fr/nc/42");
    }

    @Test
    @DisplayName("Aucune syntaxe Thymeleaf ne subsiste dans le corps rendu")
    void gabaritLivre_aucuneSyntaxeThymeleafResiduelle() throws Exception {
        String gabarit = gabaritLivre("validationNonConformite");

        // Le gabarit brut porte « [[${fullName}]] » et « th:href » : c'est exactement ce que le
        // destinataire recevait, l'ancien rendu ne touchant qu'à d'hypothétiques {{clé}}.
        assertThat(gabarit).contains("[[${").contains("th:href");

        String corps = rendu.corps(gabarit,
                Map.of("fullName", "Claire Martin", "numeroNc", "NC-1", "link", "https://exemple.fr/x"));

        assertThat(corps)
                .as("le message partait avec ses expressions Thymeleaf non interprétées")
                .doesNotContain("[[${")
                .doesNotContain("th:href")
                .doesNotContain("xmlns:th");
    }

    @Test
    @DisplayName("Une variable non fournie ne laisse pas de marqueur visible")
    void variableAbsente_aucunMarqueurVisible() throws Exception {
        String corps = rendu.corps(gabaritLivre("emailPlanAction"), Map.of("fullName", "Claire Martin"));

        assertThat(corps).doesNotContain("${").doesNotContain("{{");
    }

    // ------------------------------------------------------------------ robustesse et sécurité

    @Test
    @DisplayName("Les valeurs sont échappées : un commentaire balisé ne s'injecte pas dans le message")
    void valeurs_echappees() {
        String corps = rendu.corps("<p th:text=\"${observation}\">commentaire</p>",
                Map.of("observation", "<script>alert('xss')</script>"));

        assertThat(corps)
                .as("le remplacement littéral recopiait la valeur telle quelle dans le HTML")
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("Un gabarit invalide laisse partir un message neutre, jamais le gabarit brut")
    void gabaritInvalide_messageNeutre() {
        String gabarit = "<p th:text=\"${ceci n'est pas une expression}\">x</p><a href=\"https://ailleurs.example\">piège</a>";

        // Un gabarit est éditable par API, donc faillible : un responsable doit être prévenu
        // même par un message imparfait — mais pas par le balisage interne du modèle.
        String corps = rendu.corps(gabarit, Map.of("fullName", "Claire <b>Martin</b>",
                "reference", "NC-2026-014", "link", "https://qualite.exemple.fr/nc/42"));

        assertThat(corps)
                .doesNotContain("th:text")
                .doesNotContain("ailleurs.example")
                .contains("Claire &lt;b&gt;Martin&lt;/b&gt;")
                .contains("NC-2026-014")
                .contains("https://qualite.exemple.fr/nc/42");
    }

    @Test
    @DisplayName("Le message neutre ne reprend pas un lien qui n'est pas http(s)")
    void messageNeutre_lienNonWebIgnore() {
        String corps = RenduDesCourriels.corpsDeSecours(Map.of("link", "javascript:alert(1)"));

        assertThat(corps).doesNotContain("javascript").doesNotContain("href");
    }

    @Test
    @DisplayName("Un corps vide ou nul n'entraîne pas d'erreur")
    void corpsVide_pasDerreur() {
        assertThat(rendu.corps(null, Map.of())).isEmpty();
        assertThat(rendu.corps("  ", null)).isEmpty();
    }

    // ------------------------------------------------------------------ pied de courriel

    @Test
    @DisplayName("Le pied de l'organisation suit le corps rendu")
    void piedDeCourriel_ajouteAuCorps() {
        ReglagesOrganisation reglages = org.mockito.Mockito.mock(ReglagesOrganisation.class);
        org.mockito.Mockito.when(reglages.valeurs()).thenReturn(Map.of("ORGANISATION_NOM", "QualiSira"));
        RenduDesCourriels avecPied = new RenduDesCourriels(new EmailTemplateEngineConfig().moteurGabaritEmail(),
                new PiedDeCourriel(reglages));

        String corps = avecPied.corps("<p th:text=\"${fullName}\">x</p>", Map.of("fullName", "Claire Martin"));

        // La signature vient des réglages : l'écrire dans chaque gabarit l'aurait figée en quinze
        // exemplaires. Elle s'ajoute après le gabarit, sans remplacer son contenu.
        assertThat(corps).contains("Claire Martin");
        assertThat(corps).contains("QualiSira");
        assertThat(corps.indexOf("Claire Martin")).isLessThan(corps.indexOf("QualiSira"));
    }

    // ------------------------------------------------------------------ syntaxe {variable}

    @Test
    @DisplayName("{variable} dans le corps est substitué, avec le même échappement que Thymeleaf")
    void accolades_substituees() {
        String corps = rendu.corps("<p>Dossier {numeroNc}, commentaire : {observation}</p>",
                Map.of("numeroNc", "NC-2026-014", "observation", "<script>alert('x')</script>"));

        assertThat(corps).contains("Dossier NC-2026-014");
        // La réécriture passe par Thymeleaf : une valeur saisie ne peut pas injecter de balisage.
        assertThat(corps).doesNotContain("<script>");
        assertThat(corps).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("Les accolades du CSS et un {mot} inconnu ne sont pas touchés")
    void accolades_etrangeresIntactes() {
        String corps = rendu.corps("<style>p { margin: 0; }</style><p>{inconnu} — dossier {numeroNc}</p>",
                Map.of("numeroNc", "NC-7"));

        // Seules les variables fournies sont réécrites : le CSS du gabarit reste du CSS.
        assertThat(corps).contains("p { margin: 0; }");
        assertThat(corps).contains("{inconnu}");
        assertThat(corps).contains("dossier NC-7");
    }

    // ------------------------------------------------------------------ objet

    @Test
    @DisplayName("L'objet substitue {variable} et reste du texte d'une seule ligne")
    void sujet_substitue() {
        assertThat(RenduDesCourriels.sujet("Validation attendue - NC {numeroNc}",
                Map.of("numeroNc", "NC-2026-014")))
                .isEqualTo("Validation attendue - NC NC-2026-014");
        // Une valeur avec retour à la ligne ne peut pas scinder l'en-tête.
        assertThat(RenduDesCourriels.sujet("Objet {observation}",
                Map.of("observation", "ligne1\r\nligne2")))
                .isEqualTo("Objet ligne1 ligne2");
    }

    @Test
    @DisplayName("Une référence absente s'efface de l'objet, séparateur compris")
    void sujet_referenceAbsente() {
        // « Validation attendue – NC  » avec un vide en fin d'objet trahirait le gabarit ;
        // l'objet redevient simplement ce qu'il était.
        assertThat(RenduDesCourriels.sujet("Non-Conformité transmise – {numeroNc}", Map.of()))
                .isEqualTo("Non-Conformité transmise");
        assertThat(RenduDesCourriels.sujet("Validation requise - Clôture NC {numeroNc}",
                java.util.Collections.singletonMap("numeroNc", null)))
                .isEqualTo("Validation requise - Clôture NC");
    }

    @Test
    @DisplayName("Un objet nul devient une chaîne vide")
    void sujet_nul() {
        assertThat(RenduDesCourriels.sujet(null, Map.of("numeroNc", "NC-1"))).isEmpty();
    }
}
