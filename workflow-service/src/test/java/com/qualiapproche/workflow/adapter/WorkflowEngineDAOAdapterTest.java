package com.qualiapproche.workflow.adapter;

import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.BeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Construction du catalogue à partir de la base.
 *
 * <p>Ce chargement est rejoué au démarrage de chaque instance et à chaque modification d'un
 * circuit. Il s'appuyait sur les collections différées des entités — une lecture par circuit
 * pour ses étapes, puis une par étape pour ses transitions ; les tests fixent ici le nombre de
 * requêtes autant que le résultat.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowEngineDAOAdapterTest {

    @Mock private WorkflowRepository workflowRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowConditionAdapter conditionAdapter;
    @Mock private BeanFactory beanFactory;

    private WorkflowEngineDAOAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WorkflowEngineDAOAdapter(workflowRepository, transitionRepository,
                conditionAdapter, beanFactory);
        when(beanFactory.getBean(anyString(), any(Class.class)))
                .thenReturn(new com.qualiapproche.workflow.adapter.action.WorkflowStepAction());
    }

    /** Circuit à deux étapes : « Rédaction » puis « Vérification ». */
    private Workflow circuitADeuxEtapes() {
        Workflow aCircuit = Workflow.builder().nom("Validation standard").resourceType("DOCUMENT").build();
        aCircuit.setId(UUID.randomUUID());

        WorkflowStep aRedaction = new WorkflowStep();
        aRedaction.setId(1L);
        aRedaction.setCode("REDACTION");
        aRedaction.setNomEtape("Rédaction");
        aRedaction.setStepOrder(1);
        aRedaction.setResponsableRole("REDACTEUR");
        aRedaction.setWorkflow(aCircuit);

        WorkflowStep aVerification = new WorkflowStep();
        aVerification.setId(2L);
        aVerification.setCode("VERIFICATION");
        aVerification.setNomEtape("Vérification");
        aVerification.setStepOrder(2);
        aVerification.setResponsableRole("VERIFICATEUR");
        aVerification.setWorkflow(aCircuit);

        aCircuit.setSteps(new ArrayList<>(List.of(aRedaction, aVerification)));
        return aCircuit;
    }

    private WorkflowTransition transition(Long id, WorkflowStep origine, WorkflowStep destination,
                                          StepDecision decision, boolean terminale) {
        return WorkflowTransition.builder()
                .id(id).fromStep(origine).toStep(destination)
                .decision(decision).terminal(terminale).build();
    }

    @Test
    @DisplayName("Le catalogue complet est chargé en deux requêtes, quel que soit le nombre d'étapes")
    void catalogue_deuxRequetes() throws Exception {
        Workflow aCircuit = circuitADeuxEtapes();
        WorkflowStep aRedaction = aCircuit.getSteps().get(0);
        WorkflowStep aVerification = aCircuit.getSteps().get(1);

        when(workflowRepository.findAllAvecEtapes()).thenReturn(List.of(aCircuit));
        when(transitionRepository.findAll()).thenReturn(List.of(
                transition(10L, aRedaction, aVerification, StepDecision.APPROUVE, false),
                transition(11L, aVerification, null, StepDecision.APPROUVE, true)));

        List<WorkflowPersistant> aCatalogue = adapter.getAllWorkflow();

        assertThat(aCatalogue).hasSize(1);
        verify(workflowRepository, times(1)).findAllAvecEtapes();
        verify(transitionRepository, times(1)).findAll();
        // findAll() sans graphe laissait chaque collection se charger à la demande.
        verify(workflowRepository, never()).findAll();
    }

    @Test
    @DisplayName("Les transitions sont rattachées à leur étape d'origine")
    void transitions_rattacheesALeurEtapeDorigine() throws Exception {
        Workflow aCircuit = circuitADeuxEtapes();
        WorkflowStep aRedaction = aCircuit.getSteps().get(0);
        WorkflowStep aVerification = aCircuit.getSteps().get(1);

        when(workflowRepository.findAllAvecEtapes()).thenReturn(List.of(aCircuit));
        when(transitionRepository.findAll()).thenReturn(List.of(
                transition(10L, aRedaction, aVerification, StepDecision.APPROUVE, false),
                transition(12L, aVerification, aRedaction, StepDecision.REJETE, false)));

        WorkflowPersistant aResultat = adapter.getAllWorkflow().get(0);

        assertThat(aResultat.getTransitions()).hasSize(2);
        assertThat(aResultat.getTransitionsFromEtat(aResultat.getEtat("1")))
                .extracting(t -> t.getCode()).containsExactly("10");
        assertThat(aResultat.getTransitionsFromEtat(aResultat.getEtat("2")))
                .extracting(t -> t.getCode()).containsExactly("12");
    }

    @Test
    @DisplayName("L'état initial est la première étape du circuit")
    void etatInitial_premiereEtape() throws Exception {
        Workflow aCircuit = circuitADeuxEtapes();
        when(workflowRepository.findAllAvecEtapes()).thenReturn(List.of(aCircuit));
        when(transitionRepository.findAll()).thenReturn(List.of());

        WorkflowPersistant aResultat = adapter.getAllWorkflow().get(0);

        assertThat(aResultat.getEtatInitial().getCode()).isEqualTo("1");
        assertThat(aResultat.getEtatInitial().getLibelle()).isEqualTo("Rédaction");
    }

    @Test
    @DisplayName("Une transition terminale produit un état de sortie synthétique")
    void transitionTerminale_etatDeSortieSynthetise() throws Exception {
        Workflow aCircuit = circuitADeuxEtapes();
        WorkflowStep aVerification = aCircuit.getSteps().get(1);

        when(workflowRepository.findAllAvecEtapes()).thenReturn(List.of(aCircuit));
        when(transitionRepository.findAll()).thenReturn(List.of(
                transition(11L, aVerification, null, StepDecision.APPROUVE, true)));

        WorkflowPersistant aResultat = adapter.getAllWorkflow().get(0);

        assertThat(aResultat.getEtat("TERMINATED_APPROUVE")).isNotNull();
        assertThat(aResultat.getTransitionsFromEtat(aResultat.getEtat("2")))
                .singleElement()
                .satisfies(t -> assertThat(t.getEtatDestination().getCode()).isEqualTo("TERMINATED_APPROUVE"));
    }

    @Test
    @DisplayName("Une transition sans destination ni fin de circuit déclarée est ignorée")
    void transitionNonConfiguree_ignoree() throws Exception {
        Workflow aCircuit = circuitADeuxEtapes();
        WorkflowStep aVerification = aCircuit.getSteps().get(1);

        when(workflowRepository.findAllAvecEtapes()).thenReturn(List.of(aCircuit));
        when(transitionRepository.findAll()).thenReturn(List.of(
                transition(13L, aVerification, null, StepDecision.REJETE, false)));

        WorkflowPersistant aResultat = adapter.getAllWorkflow().get(0);

        assertThat(aResultat.getTransitions())
                .as("proposée, elle clôturerait le dossier alors que personne ne l'a voulu")
                .isEmpty();
    }

    @Test
    @DisplayName("À défaut d'habilitation propre, la transition reprend le rôle responsable de son étape")
    void habilitation_repliSurLeRoleDeLetape() throws Exception {
        Workflow aCircuit = circuitADeuxEtapes();
        WorkflowStep aRedaction = aCircuit.getSteps().get(0);
        WorkflowStep aVerification = aCircuit.getSteps().get(1);

        when(workflowRepository.findAllAvecEtapes()).thenReturn(List.of(aCircuit));
        when(transitionRepository.findAll()).thenReturn(List.of(
                transition(10L, aRedaction, aVerification, StepDecision.APPROUVE, false)));

        WorkflowPersistant aResultat = adapter.getAllWorkflow().get(0);

        assertThat(aResultat.getTransitions()).singleElement()
                .satisfies(t -> assertThat(t.getPermission()).isEqualTo("REDACTEUR"));
    }

    @Test
    @DisplayName("Une transition d'un autre circuit n'est pas rattachée")
    void transitionDunAutreCircuit_nonRattachee() throws Exception {
        Workflow aCircuit = circuitADeuxEtapes();
        WorkflowStep aRedaction = aCircuit.getSteps().get(0);

        WorkflowStep aEtapeEtrangere = new WorkflowStep();
        aEtapeEtrangere.setId(99L);
        aEtapeEtrangere.setNomEtape("Étape d'un autre circuit");

        when(workflowRepository.findAllAvecEtapes()).thenReturn(List.of(aCircuit));
        when(transitionRepository.findAll()).thenReturn(List.of(
                transition(10L, aRedaction, aCircuit.getSteps().get(1), StepDecision.APPROUVE, false),
                transition(90L, aEtapeEtrangere, aEtapeEtrangere, StepDecision.APPROUVE, false)));

        WorkflowPersistant aResultat = adapter.getAllWorkflow().get(0);

        // Toutes les transitions sont chargées d'un coup : le regroupement par étape d'origine
        // doit donc écarter celles qui relèvent d'un autre circuit.
        assertThat(aResultat.getTransitions()).extracting(t -> t.getCode()).containsExactly("10");
    }
}
