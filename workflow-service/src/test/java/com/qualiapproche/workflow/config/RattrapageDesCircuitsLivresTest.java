package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.SeveriteAction;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rattrapage des circuits déjà en base, quand le circuit livré s'enrichit.
 *
 * <p>L'initialiseur ne crée un circuit que si aucun n'existe : toute étape ajoutée après la mise en
 * service restait invisible partout sauf sur une base vierge, et la seule issue était de supprimer
 * le circuit — c'est-à-dire de perdre l'historique de tous les dossiers qu'il pilote.</p>
 */
class RattrapageDesCircuitsLivresTest {

    private WorkflowRepository workflowRepository;
    private RattrapageDesCircuitsLivres rattrapage;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        rattrapage = new RattrapageDesCircuitsLivres(workflowRepository);
        when(workflowRepository.findByResourceType(any())).thenReturn(List.of());
    }

    private void enBase(String typeRessource, Workflow circuit) {
        when(workflowRepository.findByResourceType(typeRessource)).thenReturn(List.of(circuit));
    }

    private WorkflowStep etape(Workflow circuit, String code) {
        return circuit.getSteps().stream()
                .filter(s -> code.equals(s.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Étape absente : " + code));
    }

    /** Le circuit livré, amputé d'une étape et de la route qui la traverse : une base d'avant. */
    private Workflow circuitNonConformiteDAvant() {
        Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        WorkflowStep validationRq = etape(circuit, "VALIDATION_RQ");
        WorkflowStep imputation = etape(circuit, "IMPUTATION");

        // La réception menait directement à l'imputation, sans validation qualité.
        etape(circuit, "RECEPTION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .forEach(t -> t.setToStep(imputation));
        circuit.getSteps().remove(validationRq);
        return circuit;
    }

    @Test
    @DisplayName("Une étape absente du circuit en base y est ajoutée")
    void etapeManquante_ajoutee() {
        Workflow circuit = circuitNonConformiteDAvant();
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        assertThat(circuit.getSteps()).extracting(WorkflowStep::getCode).contains("VALIDATION_RQ");
        verify(workflowRepository).save(circuit);
    }

    @Test
    @DisplayName("Les routes existantes passent par l'étape ajoutée au lieu de l'enjamber")
    void routes_traversentLEtapeAjoutee() {
        Workflow circuit = circuitNonConformiteDAvant();
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Sans cela l'étape serait dans le circuit, visible dans l'éditeur, et jamais atteinte par
        // un seul dossier.
        WorkflowTransition approbation = etape(circuit, "RECEPTION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .findFirst().orElseThrow();
        assertThat(approbation.getToStep().getCode()).isEqualTo("VALIDATION_RQ");
    }

    @Test
    @DisplayName("Un champ ajouté au circuit livré atteint les circuits déjà en base")
    void champManquant_ajoute() {
        // Une étape complète mais sans point de saisie laisse passer la décision et perd ce qu'elle
        // devait justifier : le compte rendu du responsable n'irait nulle part.
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        etape(circuit, "NON_TRAITER").getFields().clear();
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        assertThat(etape(circuit, "NON_TRAITER").getFields())
                .extracting(WorkflowStepField::getFieldName)
                .contains("causeIdentifiees", "solutionRetenues");
        verify(workflowRepository).save(circuit);
    }

    @Test
    @DisplayName("Un champ déjà porté par l'étape n'est pas redéfini")
    void champExistant_preserve() {
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        WorkflowStep aRealiser = etape(circuit, "NON_TRAITER");
        aRealiser.getFields().removeIf(f -> f.getFieldName().equals("solutionRetenues"));
        WorkflowStepField personnalise = aRealiser.getFields().stream()
                .filter(f -> f.getFieldName().equals("causeIdentifiees"))
                .findFirst().orElseThrow();
        personnalise.setFieldLabel("Origine du problème");
        personnalise.setRequired(false);
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        // L'administrateur a pu changer le libellé ou la portée : ce n'est pas à un rattrapage de
        // défaire son choix. Seul ce qui manque est ajouté.
        assertThat(personnalise.getFieldLabel()).isEqualTo("Origine du problème");
        assertThat(personnalise.isRequired()).isFalse();
        assertThat(aRealiser.getFields())
                .extracting(WorkflowStepField::getFieldName)
                .contains("solutionRetenues");
    }

    @Test
    @DisplayName("Un circuit déjà complet n'est pas réenregistré")
    void circuitComplet_intact() {
        enBase("NON_CONFORMITE", WorkflowDataInitializer.circuitNonConformiteParDefaut());
        enBase("PLAN_ACTION", WorkflowDataInitializer.circuitPlanActionParDefaut());

        rattrapage.run();

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un libellé personnalisé n'est pas défait")
    void personnalisation_preservee() {
        Workflow circuit = circuitNonConformiteDAvant();
        WorkflowTransition approbation = etape(circuit, "RECEPTION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .findFirst().orElseThrow();
        approbation.setLabel("Prendre en charge");
        approbation.setSeverity(SeveriteAction.INFO);
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // La route est la structure du circuit et doit suivre ; l'apparence du bouton est le choix
        // de l'administrateur et n'a pas à être défait.
        assertThat(approbation.getLabel()).isEqualTo("Prendre en charge");
        assertThat(approbation.getSeverity()).isEqualTo(SeveriteAction.INFO);
    }

    @Test
    @DisplayName("Un circuit recomposé à la main est laissé intact")
    void circuitRecompose_intact() {
        Workflow circuit = circuitNonConformiteDAvant();
        circuit.addStep(WorkflowStep.builder()
                .code("ARBITRAGE_DIRECTION").nomEtape("Arbitrage").stepOrder(20)
                .responsableRole("PILOTE").build());
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Y greffer les étapes livrées mêlerait deux conceptions du circuit, et le résultat ne
        // serait celui de personne.
        assertThat(circuit.getSteps()).extracting(WorkflowStep::getCode).doesNotContain("VALIDATION_RQ");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une étape de traitement ouverte à un rôle est rendue à son titulaire")
    void traitementOuvertAUnRole_rendueAuTitulaire() {
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        WorkflowStep aTraiter = etape(circuit, "NON_TRAITER");
        aTraiter.setResponsableRole("AGENT");
        aTraiter.getTransitions().forEach(t -> t.setRequiredRole(null));
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        // Ouverte au rôle, l'étape laissait tout agent solder le plan d'un autre, et par là ouvrir
        // la clôture d'une non-conformité qui ne le concernait pas.
        assertThat(aTraiter.getResponsableRole()).isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE);
        assertThat(aTraiter.getTransitions())
                .allSatisfy(t -> assertThat(t.getRequiredRole())
                        .isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE));
        verify(workflowRepository).save(circuit);
    }

    @Test
    @DisplayName("Une étape qui doit nommer quelqu'un reçoit le champ qui le désigne")
    void champTitulaire_ajoute() {
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        WorkflowStep rejete = etape(circuit, "REJECTED");
        String champ = rejete.getChampTitulaire();
        rejete.setChampTitulaire(null);
        rejete.getFields().clear();
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        // Sans le champ, l'étape serait réservée à une personne que rien ne permet de nommer, et
        // deviendrait indécidable.
        assertThat(rejete.getChampTitulaire()).isEqualTo(champ);
        assertThat(rejete.getFields()).extracting(f -> f.getFieldName()).contains(champ);
    }
}
