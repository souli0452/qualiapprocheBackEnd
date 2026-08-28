package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que les circuits livrés annoncent par courriel, et à qui.
 *
 * <p>Un gabarit désigné mais absent ne se voit pas : le notificateur consigne « modèle d'e-mail
 * introuvable » et se tait, l'étape est franchie, et celui qu'on attend n'apprend jamais qu'un
 * dossier lui revient. Une faute de frappe dans un code de gabarit coûte donc une notification
 * entière, sans qu'aucune erreur ne remonte à personne.</p>
 *
 * <p>Le second point ne relève pas du même ordre : les circuits documentaires empruntaient les
 * gabarits des non-conformités, et disaient donc autre chose que ce qui se passait — le rédacteur
 * d'une procédure recevait « Nouvelle Non-Conformité imputée », et le responsable qualité à qui un
 * document était transmis, « Validation attendue - Non-Conformité ». Le message partait bien ; il
 * était faux.</p>
 */
class CourrielsDesCircuitsLivresTest {

    /** Les quatre circuits livrés, par le type de ressource qu'ils pilotent. */
    private static final Map<String, Supplier<Workflow>> CIRCUITS = Map.of(
            "NON_CONFORMITE", WorkflowDataInitializer::circuitNonConformiteParDefaut,
            "PLAN_ACTION", WorkflowDataInitializer::circuitPlanActionParDefaut,
            "DOCUMENT", WorkflowDataInitializer::circuitDocumentParDefaut,
            "DEMANDE_DOCUMENT", WorkflowDataInitializer::circuitDemandeDocumentParDefaut);

    /**
     * Gabarits génériques d'avant la refonte : ils parlent tous de non-conformité, et aucun circuit
     * livré ne doit plus s'y référer. Ils restent en base pour les circuits recomposés à la main.
     */
    private static final Set<String> GABARITS_DE_LA_NON_CONFORMITE = Set.of(
            "emailTemplate", "structureToStructure", "validationNonConformite", "validationRq",
            "rejectNonConformite", "succesTraitementNonformite", "traitementReussi",
            "emailPlanAction", "validationPlanRequise", "emailRqPlan", "validationAfterPlan");

    @Test
    @DisplayName("Chaque gabarit désigné par un circuit livré existe au classpath")
    void gabaritsDesignes_tousPresents() {
        for (Map.Entry<String, Supplier<Workflow>> circuit : CIRCUITS.entrySet()) {
            for (WorkflowStep etape : circuit.getValue().get().getSteps()) {
                String gabarit = etape.getEmailTemplateCode();
                if (gabarit == null || gabarit.isBlank()) {
                    continue;
                }
                assertThat(new ClassPathResource("templates/" + gabarit + ".html").exists())
                        .as("circuit %s, étape %s : le gabarit « %s » doit exister",
                                circuit.getKey(), etape.getCode(), gabarit)
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("Chaque étape d'un circuit livré annonce son franchissement")
    void chaqueEtape_annonceSonFranchissement() {
        for (Map.Entry<String, Supplier<Workflow>> circuit : CIRCUITS.entrySet()) {
            for (WorkflowStep etape : circuit.getValue().get().getSteps()) {
                assertThat(etape.getEmailTemplateCode())
                        .as("circuit %s, étape %s : une étape sans gabarit est atteinte en silence",
                                circuit.getKey(), etape.getCode())
                        .isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("Les circuits documentaires n'empruntent plus les gabarits de la non-conformité")
    void circuitsDocumentaires_gabaritsPropres() {
        for (String type : List.of("DOCUMENT", "DEMANDE_DOCUMENT")) {
            for (WorkflowStep etape : CIRCUITS.get(type).get().getSteps()) {
                assertThat(etape.getEmailTemplateCode())
                        .as("circuit %s, étape %s", type, etape.getCode())
                        .isNotIn(GABARITS_DE_LA_NON_CONFORMITE);
            }
        }
    }

    @Test
    @DisplayName("Le document retourné s'annonce à son rédacteur, non à tous les agents du processus")
    void documentRetourne_sAdresseASonAuteur() {
        WorkflowStep redaction = etape("DOCUMENT", "REDACTION");

        // L'étape reste ouverte au rôle : n'importe quel rédacteur du processus peut reprendre le
        // brouillon. Le courriel, lui, s'adresse à celui qui a déposé — les deux questions sont
        // distinctes, et les confondre écrivait « votre document vous est retourné » à des agents
        // qui n'avaient rien écrit.
        assertThat(redaction.getResponsableRole()).isEqualTo("AGENT");
        assertThat(redaction.getDestinataireCourriel()).isEqualTo("@CREATEUR");
        assertThat(redaction.getEmailTemplateCode()).isEqualTo("documentRenvoyeAuRedacteur");
    }

    @Test
    @DisplayName("La demande renvoyée trouve son auteur par l'habilitation de l'étape, sans désignation")
    void demandeRenvoyee_auteurParLHabilitation() {
        WorkflowStep soumission = etape("DEMANDE_DOCUMENT", "DEMANDE_SOUMISSION");

        // Ici l'étape est déjà réservée à l'auteur : la désignation ferait double emploi.
        assertThat(soumission.getResponsableRole()).isEqualTo("@CREATEUR");
        assertThat(soumission.getDestinataireCourriel()).isNull();
        assertThat(soumission.getEmailTemplateCode()).isEqualTo("demandeRenvoyeeAuDemandeur");
    }

    private WorkflowStep etape(String typeRessource, String code) {
        return CIRCUITS.get(typeRessource).get().getSteps().stream()
                .filter(step -> code.equals(step.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Étape " + code + " absente du circuit " + typeRessource));
    }
}
