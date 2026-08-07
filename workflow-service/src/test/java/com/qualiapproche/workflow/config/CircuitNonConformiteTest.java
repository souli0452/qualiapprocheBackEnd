package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le circuit de non-conformité livré : ce qu'il permet, et à qui.
 *
 * <p>Trois propriétés ne se voient pas à la lecture et se paient cher à l'usage : une étape sans
 * issue de rejet enferme le dossier dans l'avancement, une étape habilitée par un rôle mal choisi
 * l'ouvre à qui ne devrait pas décider, et une étape de traitement ouverte par rôle plutôt que par
 * désignation laisse chacun traiter le dossier d'un autre.</p>
 */
class CircuitNonConformiteTest {

    /** Les seuls rôles de la plateforme. « Agent imputé » n'en est pas un. */
    private static final Set<String> ROLES_ADMIS = Set.of("AGENT", "PILOTE", "RESPONSABLE_QUALITE");

    /**
     * Valeurs admises dans la colonne « rôle responsable » : les trois rôles, ou l'une des deux
     * habilitations qui désignent une personne — le titulaire du dossier, son créateur.
     */
    private static final Set<String> HABILITATIONS_ADMISES =
            Set.of("AGENT", "PILOTE", "RESPONSABLE_QUALITE",
                    WorkflowDataInitializer.HABILITATION_TITULAIRE,
                    WorkflowDataInitializer.HABILITATION_CREATEUR);

    private final Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();

    private WorkflowStep etape(String code) {
        return circuit.getSteps().stream()
                .filter(step -> code.equals(step.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Étape absente du circuit : " + code));
    }

    private WorkflowTransition rejetDe(String code) {
        return etape(code).getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.REJETE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun rejet possible à l'étape " + code));
    }

    @Test
    @DisplayName("Le pilote ne valide pas un traitement dont une action n'a pas de responsable")
    void validationPilote_exigeLAffectation() {
        WorkflowTransition validation = etape("VALIDATION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le pilote ne peut pas valider le traitement."));

        // Une action sans responsable ne serait confiée à personne à la validation qualité, et le
        // dossier attendrait indéfiniment le solde d'une action que nul n'a commencée.
        assertThat(validation.getConditionRequise()).isEqualTo("PLANS_ACTION_AFFECTES");
        assertThat(validation.getConditionLibelle())
                .withFailMessage("Sans phrase, l'écran ne peut afficher que le nom technique du fait.")
                .isNotBlank();
    }

    @Test
    @DisplayName("Toute condition posée s'explique en clair")
    void conditions_toutesExpliquees() {
        for (WorkflowStep etape : circuit.getSteps()) {
            assertThat(etape.getTransitions())
                    .filteredOn(t -> t.getConditionRequise() != null)
                    .allSatisfy(t -> assertThat(t.getConditionLibelle())
                            .withFailMessage("La décision %s de l'étape « %s » pose une condition que "
                                    + "rien n'explique : l'utilisateur verrait le dossier arrêté sans "
                                    + "savoir ce qu'il attend.", t.getDecision(), etape.getCode())
                            .isNotBlank());
        }
    }

    @Test
    @DisplayName("Aucune étape n'emploie un rôle qui n'existe pas")
    void roles_tousReels() {
        assertThat(circuit.getSteps())
                .filteredOn(step -> step.getResponsableRole() != null)
                .extracting(WorkflowStep::getResponsableRole)
                .withFailMessage("Le référentiel ne crée que %s : un autre nom n'habiliterait "
                        + "personne, et l'étape serait indécidable.", ROLES_ADMIS)
                .allMatch(HABILITATIONS_ADMISES::contains);
    }

    @Test
    @DisplayName("Le traitement n'appartient à aucun rôle : il revient à la personne imputée")
    void traitement_reserveAuTitulaire() {
        WorkflowStep traitement = etape("TRAITEMENT");

        // Un rôle « agent imputé » aurait ouvert le traitement de tout dossier à tout agent
        // imputable : c'est une personne sur un dossier, pas une catégorie d'utilisateurs.
        assertThat(traitement.getResponsableRole())
                .isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE);
        assertThat(traitement.getTransitions())
                .allSatisfy(t -> assertThat(t.getRequiredRole())
                        .isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE));
    }

    @Test
    @DisplayName("L'imputation nomme le titulaire du dossier")
    void imputation_designeLeTitulaire() {
        WorkflowStep imputation = etape("IMPUTATION");

        assertThat(imputation.getChampTitulaire()).isEqualTo(WorkflowDataInitializer.CHAMP_AGENT_IMPUTE);
        assertThat(imputation.getFields()).extracting("fieldName")
                .withFailMessage("L'étape déclare désigner le titulaire par un champ qu'elle "
                        + "n'offre pas à la saisie : personne ne serait jamais nommé.")
                .contains(WorkflowDataInitializer.CHAMP_AGENT_IMPUTE);
    }

    @Test
    @DisplayName("Toute étape après la déclaration offre une issue de rejet")
    void rejet_possibleAChaqueEtape() {
        List<String> avecRejetAttendu = List.of(
                "RECEPTION", "VALIDATION_RQ", "IMPUTATION", "TRAITEMENT",
                "VALIDATION", "VALIDATION_RS", "SUIVI_RQ");

        assertThat(avecRejetAttendu).allSatisfy(code ->
                assertThat(rejetDe(code)).as("rejet attendu à l'étape %s", code).isNotNull());
    }

    @Test
    @DisplayName("Le justificatif ne se demande qu'au rejet, jamais à l'approbation")
    void justificatif_rattacheAuRejet() {
        // Attaché à l'étape sans plus de précision, il s'affichait aussi quand l'utilisateur
        // approuvait : on lui demandait de motiver un refus qu'il n'était pas en train de prononcer.
        assertThat(circuit.getSteps())
                .flatExtracting(WorkflowStep::getFields)
                .filteredOn(champ -> WorkflowDataInitializer.CHAMP_DOCUMENT_REJET.equals(champ.getFieldName()))
                .isNotEmpty()
                .allSatisfy(champ -> assertThat(champ.getDecision()).isEqualTo(StepDecision.REJETE));
    }

    @Test
    @DisplayName("L'affectation à une structure se décide à la validation RQ, et là seulement")
    void affectation_decideeParLeResponsableQualite() {
        WorkflowStep validationRq = etape("VALIDATION_RQ");

        assertThat(validationRq.getResponsableRole()).isEqualTo("RESPONSABLE_QUALITE");
        // Exigée : approuver sans désigner laisserait le dossier à une étape d'imputation que le
        // pilote d'aucune structure ne se reconnaîtrait tenu de traiter.
        assertThat(validationRq.getFields())
                .filteredOn(champ -> WorkflowDataInitializer.CHAMP_STRUCTURE_DESTINATAIRE_ID
                        .equals(champ.getFieldName()))
                .singleElement()
                .satisfies(champ -> assertThat(champ.isRequired()).isTrue());

        // Nulle part ailleurs : deux étapes d'aiguillage rendraient impossible de dire qui a
        // orienté le dossier.
        assertThat(circuit.getSteps())
                .filteredOn(step -> !"VALIDATION_RQ".equals(step.getCode()))
                .allSatisfy(step -> assertThat(step.getFields()).extracting("fieldName")
                        .doesNotContain(WorkflowDataInitializer.CHAMP_STRUCTURE_DESTINATAIRE_ID));
    }

    @Test
    @DisplayName("La validation RQ s'intercale entre la réception et l'imputation")
    void validationRq_precedeLImputation() {
        assertThat(etape("RECEPTION").getTransitions())
                .filteredOn(t -> t.getDecision() == StepDecision.APPROUVE)
                .singleElement()
                .satisfies(t -> assertThat(t.getToStep().getCode()).isEqualTo("VALIDATION_RQ"));

        assertThat(etape("VALIDATION_RQ").getTransitions())
                .filteredOn(t -> t.getDecision() == StepDecision.APPROUVE)
                .singleElement()
                .satisfies(t -> assertThat(t.getToStep().getCode()).isEqualTo("IMPUTATION"));
    }

    @Test
    @DisplayName("La déclaration n'a pas d'issue de rejet : rien n'a encore été examiné")
    void soumission_sansRejet() {
        assertThat(etape("SOUMISSION").getTransitions())
                .extracting(WorkflowTransition::getDecision)
                .doesNotContain(StepDecision.REJETE);
    }

    @Test
    @DisplayName("Chaque rejet renvoie à l'étape qui peut y remédier")
    void rejets_renvoientLaOuLOnPeutCorriger() {
        // Un rejet qui n'irait pas là où la correction est possible obligerait à ressaisir le
        // dossier, ou le renverrait à quelqu'un qui n'a rien à corriger.
        assertThat(rejetDe("RECEPTION").getToStep().getCode()).isEqualTo("SOUMISSION");
        assertThat(rejetDe("VALIDATION_RQ").getToStep().getCode()).isEqualTo("RECEPTION");
        // Contester l'affectation, c'est la rendre à qui l'a décidée — le responsable qualité —
        // et non à la réception, qui n'a pas choisi la structure.
        assertThat(rejetDe("IMPUTATION").getToStep().getCode()).isEqualTo("VALIDATION_RQ");
        assertThat(rejetDe("TRAITEMENT").getToStep().getCode()).isEqualTo("IMPUTATION");
        assertThat(rejetDe("VALIDATION").getToStep().getCode()).isEqualTo("TRAITEMENT");
        assertThat(rejetDe("VALIDATION_RS").getToStep().getCode()).isEqualTo("TRAITEMENT");
        assertThat(rejetDe("SUIVI_RQ").getToStep().getCode()).isEqualTo("TRAITEMENT");
    }

    @Test
    @DisplayName("La clôture attend que les plans d'action soient soldés")
    void cloture_attendLesPlansDAction() {
        WorkflowTransition cloture = etape("SUIVI_RQ").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .findFirst().orElseThrow();

        // Clore une non-conformité dont les actions correctives ne sont pas menées, c'est clore une
        // non-conformité qui n'est pas corrigée.
        assertThat(cloture.getConditionRequise())
                .isEqualTo(WorkflowDataInitializer.FAIT_PLANS_ACTION_SOLDES);
    }

    @Test
    @DisplayName("Aucun rejet ne fait avancer le dossier")
    void rejets_neSautentPasEnAvant() {
        // Le code manuel renvoyait l'imputation rejetée vers la validation RQ, deux étapes plus
        // loin : le dossier aurait sauté traitement et validation sans que personne ne les fasse.
        assertThat(circuit.getSteps()).allSatisfy(step -> step.getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.REJETE)
                .filter(t -> t.getToStep() != null)
                .forEach(t -> assertThat(t.getToStep().getStepOrder())
                        .as("le rejet à l'étape %s renvoie vers %s", step.getCode(), t.getToStep().getCode())
                        .isLessThan(step.getStepOrder())));
    }
}
