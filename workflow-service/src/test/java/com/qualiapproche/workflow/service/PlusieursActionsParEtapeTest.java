package com.qualiapproche.workflow.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.dto.WorkflowTransitionDto;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import com.qualiapproche.workflow.repository.WorkflowStepFieldRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Une étape peut offrir plus de deux suites.
 *
 * <p>L'action se confondait avec sa décision : une étape n'avait donc que deux issues — approuver,
 * rejeter — et l'unicité en base l'imposait. Une étape de validation ne pouvait pas proposer
 * « Valider » à côté de « Demander un complément », qui l'une et l'autre approuvent sans mener au
 * même endroit ni attendre la même saisie. C'est le <b>code</b> de l'action qui l'identifie
 * désormais dans son étape.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlusieursActionsParEtapeTest {

    @Mock private IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur;
    @Mock private ValidationHistoryRepository historyRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private WorkflowValidationInstanceRepository validationInstanceRepository;
    @Mock private WorkflowStepFieldRepository stepFieldRepository;
    @Mock private WorkflowFieldValueRepository fieldValueRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowStepRepository stepRepository;

    private WorkflowService service;

    private final UUID workflowId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkflowService(moteur, historyRepository, eventPublisher, workflowRepository,
                validationInstanceRepository, stepFieldRepository, fieldValueRepository,
                transitionRepository, stepRepository,
                org.mockito.Mockito.mock(StructureUtilisateurService.class), null);
    }

    /** Un circuit d'une seule étape, telle qu'elle existe en base avant modification. */
    private Workflow circuitEnBase() {
        Workflow circuit = Workflow.builder().nom("Validation").resourceType("DOCUMENT").actif(true).build();
        circuit.setId(workflowId);

        WorkflowStep etape = new WorkflowStep();
        etape.setId(1L);
        etape.setCode("VALIDATION");
        etape.setNomEtape("Validation");
        etape.setStepOrder(1);
        etape.setWorkflow(circuit);
        etape.setTransitions(new ArrayList<>());

        circuit.setSteps(new ArrayList<>(List.of(etape)));
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit));
        when(workflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return circuit;
    }

    private WorkflowDto circuitPropose(WorkflowTransitionDto... actions) {
        return WorkflowDto.builder()
                .nom("Validation").resourceType("DOCUMENT")
                .steps(List.of(WorkflowStepDto.builder()
                        .id(1L).code("VALIDATION").nomEtape("Validation").stepOrder(1)
                        .transitions(List.of(actions))
                        .build()))
                .build();
    }

    @Test
    @DisplayName("Une étape retient plusieurs actions de même nature")
    void deuxApprobations_toutesRetenues() {
        Workflow circuit = circuitEnBase();

        service.updateWorkflow(workflowId, circuitPropose(
                WorkflowTransitionDto.builder().code("VALIDER").decision("APPROUVE")
                        .label("Valider").build(),
                WorkflowTransitionDto.builder().code("DEMANDER_COMPLEMENT").decision("APPROUVE")
                        .label("Demander un complément").build(),
                WorkflowTransitionDto.builder().code("REJETE").decision("REJETE")
                        .label("Refuser").build()));

        // La seconde approbation était purement et simplement écartée : l'auteur du circuit
        // l'enregistrait, l'écran la lui rendait absente, et rien ne le lui disait.
        assertThat(circuit.getSteps().get(0).getTransitions())
                .extracting(WorkflowTransition::getCode)
                .containsExactly("VALIDER", "DEMANDER_COMPLEMENT", "REJETE");
    }

    @Test
    @DisplayName("Deux actions sans code ne se recouvrent pas")
    void actionsSansCode_distinguees() {
        Workflow circuit = circuitEnBase();

        service.updateWorkflow(workflowId, circuitPropose(
                WorkflowTransitionDto.builder().decision("APPROUVE").label("Valider").build(),
                WorkflowTransitionDto.builder().decision("APPROUVE").label("Valider sous réserve").build()));

        // À défaut de code, celui de la décision — mais deux actions homonymes s'écraseraient l'une
        // l'autre à l'enregistrement suivant, et l'unicité en base refuserait la seconde.
        assertThat(circuit.getSteps().get(0).getTransitions())
                .extracting(WorkflowTransition::getCode)
                .containsExactly("APPROUVE", "APPROUVE_2");
    }

    @Test
    @DisplayName("Le code d'une action est ramené à sa forme de référence")
    void codeDeLAction_normalise() {
        Workflow circuit = circuitEnBase();

        service.updateWorkflow(workflowId, circuitPropose(
                WorkflowTransitionDto.builder().code(" demander complément ").decision("APPROUVE").build()));

        // Deux graphies du même code créeraient deux actions là où l'auteur n'en voyait qu'une.
        assertThat(circuit.getSteps().get(0).getTransitions())
                .extracting(WorkflowTransition::getCode)
                .containsExactly("DEMANDER_COMPLÉMENT");
    }

    @Test
    @DisplayName("Décider par la seule nature de l'action est refusé quand l'étape en offre plusieurs")
    void decisionAmbigue_refusee() {
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(resourceId.toString())
                .resourceType("DOCUMENT")
                .workflowCode(workflowId.toString())
                .etatCode("1")
                .status(ValidationStatus.EN_COURS)
                .build();
        when(validationInstanceRepository.findTopByResourceIdAndStatusOrderByStartedAtDesc(
                resourceId.toString(), ValidationStatus.EN_COURS)).thenReturn(Optional.of(instance));
        when(transitionRepository.findByFromStepIdAndDecision(1L, StepDecision.APPROUVE))
                .thenReturn(List.of(
                        WorkflowTransition.builder().id(10L).code("VALIDER").label("Valider")
                                .decision(StepDecision.APPROUVE).build(),
                        WorkflowTransition.builder().id(11L).code("DEMANDER_COMPLEMENT")
                                .label("Demander un complément").decision(StepDecision.APPROUVE).build()));

        // Les deux approuvent mais ne mènent pas au même endroit : en choisir une au nom de
        // l'utilisateur ferait prendre au dossier une route que personne n'a décidée.
        assertThatThrownBy(() -> service.validateStep(resourceId, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Valider")
                .hasMessageContaining("Demander un complément");
    }
}
