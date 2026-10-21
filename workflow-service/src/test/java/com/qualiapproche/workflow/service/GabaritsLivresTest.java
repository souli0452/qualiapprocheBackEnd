package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.config.EmailTemplateEngineConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les gabarits livrés, passés au moteur qui les rendra en production.
 *
 * <p>Un gabarit de courriel n'est pas compilé : une balise mal fermée ou une expression fautive ne
 * se découvre qu'au moment de l'envoi, chez le destinataire, et le rendu échoue alors en silence —
 * {@link RenduDesCourriels} rend le corps brut plutôt que de perdre la notification. Ces
 * vérifications portent donc sur les fichiers <b>réels</b> du projet, rendus par ce même service :
 * un gabarit illisible y revient brut, et ses {@code th:} et {@code ${…}} résiduels le trahissent.</p>
 *
 * <p>Trois exigences : le gabarit s'analyse, ses variables sont substituées, et rien de son
 * balisage {@code th:} ne subsiste dans le message remis.</p>
 */
class GabaritsLivresTest {

    private final RenduDesCourriels rendu = new RenduDesCourriels(
            new EmailTemplateEngineConfig().moteurGabaritEmail(), new PiedDeCourriel(reglagesVides()));

    /** Aucun réglage : le pied s'abstient, et le rendu vérifié est celui du seul gabarit. */
    private static ReglagesOrganisation reglagesVides() {
        ReglagesOrganisation reglages = mock(ReglagesOrganisation.class);
        when(reglages.valeurs()).thenReturn(Map.of());
        return reglages;
    }

    /** Toutes les variables que les gabarits du projet emploient, tous émetteurs confondus. */
    private static final Map<String, String> VALEURS = Map.ofEntries(
            Map.entry("fullName", "Awa Traoré"),
            Map.entry("link", "https://exemple.test/dossier/42"),
            Map.entry("observation", "Direction des opérations"),
            Map.entry("numeroNc", "NC-2026-0148"),
            // Noms neutres employés par les gabarits documentaires : la référence d'un document
            // n'est pas un numéro de non-conformité, et l'auteur d'une décision est nommé partout.
            Map.entry("reference", "DOC-2026-007"),
            Map.entry("auteur", "Ibrahim Ouédraogo"),
            Map.entry("etape", "Vérification"),
            Map.entry("firstName", "Awa"),
            Map.entry("lastName", "Traoré"),
            Map.entry("temporaryPassword", "Provisoire#2026"),
            Map.entry("verificationLink", "https://exemple.test/activation?jeton=abc"),
            Map.entry("resetLink", "https://exemple.test/mot-de-passe?jeton=abc"),
            Map.entry("url", "https://exemple.test/connexion"),
            Map.entry("appUrl", "https://exemple.test/qms"),
            Map.entry("documentNumber", "DOC-2026-007"),
            Map.entry("documentTitre", "Procédure de maîtrise documentaire"),
            Map.entry("documentType", "Procédure"),
            Map.entry("serviceLibelle", "Qualité"),
            Map.entry("sharedByName", "Ibrahim Ouédraogo"),
            Map.entry("userFullName", "Awa Traoré"),
            Map.entry("role", "WRITE"));

    @DisplayName("Chaque gabarit livré s'analyse, se substitue, et ne laisse aucun attribut th: au destinataire")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "emailTemplate", "structureToStructure", "validationNonConformite", "validationRq",
            "rejectNonConformite", "succesTraitementNonformite", "traitementReussi",
            "emailPlanAction", "validationPlanRequise", "emailRqPlan", "validationAfterPlan",
            "rejectPlanAction", "alertePlanAction", "alerteLastDay", "alerteEpuise",
            "verificationEmailTemplate", "reinitializePasswordEmailTemplate",
            "resetPasswordEmailTemplate", "qmsDocumentShare",
            // Un gabarit par étape des circuits de non-conformité et d'action corrective.
            "ncRecue", "ncAApprecier", "ncTransmise", "ncImputee", "ncPlanAValider",
            "ncPlanAContresigner", "ncAClore", "ncCloturee", "ncRenvoyeeAuDeclarant",
            "actionAMener", "actionAVerifier", "actionEfficaciteAMesurer", "actionSoldee",
            // Et un par étape des deux circuits documentaires.
            "documentAVerifier", "documentAApprouver", "documentRenvoyeAuRedacteur",
            "demandeAInstruire", "demandeRetenue", "demandeRenvoyeeAuDemandeur"})
    void gabaritRenduSansResidu(String code) throws IOException {
        String source = lire(code);

        String corps = rendu.corps(source, VALEURS);

        // Un attribut, donc précédé d'une espace : chercher « th: » nu désignerait aussi le
        // « max-width:600px » du squelette, et ce test échouerait sur un gabarit irréprochable.
        assertThat(corps)
                .as("aucun attribut Thymeleaf ne doit atteindre le destinataire")
                .doesNotContainPattern("\\sth:[a-zA-Z]")
                .as("aucune expression non évaluée")
                .doesNotContain("[[$").doesNotContain("${");

        // Les valeurs, elles, doivent bien être arrivées : un gabarit qui s'analyse sans rien
        // substituer passerait les vérifications ci-dessus tout en partant impersonnel.
        assertThat(corps).containsAnyOf("Awa", "DOC-2026-007", "NC-2026-0148", "exemple.test");
    }

    @DisplayName("Le squelette commun est présent partout : un courriel sans en-tête ni pied n'est pas identifiable")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "emailTemplate", "structureToStructure", "validationNonConformite", "validationRq",
            "rejectNonConformite", "succesTraitementNonformite", "traitementReussi",
            "emailPlanAction", "validationPlanRequise", "emailRqPlan", "validationAfterPlan",
            "rejectPlanAction", "alertePlanAction", "alerteLastDay", "alerteEpuise",
            "verificationEmailTemplate", "reinitializePasswordEmailTemplate",
            "resetPasswordEmailTemplate", "qmsDocumentShare",
            // Un gabarit par étape des circuits de non-conformité et d'action corrective.
            "ncRecue", "ncAApprecier", "ncTransmise", "ncImputee", "ncPlanAValider",
            "ncPlanAContresigner", "ncAClore", "ncCloturee", "ncRenvoyeeAuDeclarant",
            "actionAMener", "actionAVerifier", "actionEfficaciteAMesurer", "actionSoldee",
            // Et un par étape des deux circuits documentaires.
            "documentAVerifier", "documentAApprouver", "documentRenvoyeAuRedacteur",
            "demandeAInstruire", "demandeRetenue", "demandeRenvoyeeAuDemandeur"})
    void squeletteCommun(String code) throws IOException {
        String source = lire(code);

        assertThat(source)
                .as("largeur fixée : sans elle, le message s'étale sur toute la fenêtre")
                .contains("max-width:600px")
                .as("mise en page en tableaux : Outlook ignore les mises en page en blocs")
                .contains("role=\"presentation\"")
                .as("texte d'aperçu affiché dans la liste des messages")
                .contains("mso-hide:all")
                .as("en-tête de marque").contains("quali-sira")
                .as("mention d'envoi automatique").contains("Merci de ne pas répondre");

        assertThat(source)
                .as("aucune propriété qu'Outlook laisse tomber en silence")
                .doesNotContain("display: flex").doesNotContain("linear-gradient")
                .doesNotContain("box-shadow")
                .as("aucun astérisque Markdown resté dans le texte destiné au lecteur")
                .doesNotContain("**");
    }

    private String lire(String code) throws IOException {
        ClassPathResource fichier = new ClassPathResource("templates/" + code + ".html");
        assertThat(fichier.exists()).as("gabarit %s présent au classpath", code).isTrue();
        return StreamUtils.copyToString(fichier.getInputStream(), StandardCharsets.UTF_8);
    }
}
