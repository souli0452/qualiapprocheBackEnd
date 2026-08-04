package com.qualiapproche.workflow.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.dto.WorkflowStateDto;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Consultation de l'état de validation, unitaire et par lot.
 *
 * <p>L'essentiel porte ici sur le <b>nombre de requêtes</b> : c'est la propriété que ces tests
 * verrouillent, et la seule qui distingue la lecture par lot d'une boucle sur la lecture
 * unitaire. Sans elle, l'endpoint tenait sa promesse d'un appel HTTP unique tout en conservant
 * son coût par ressource.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceLotTest {

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

    private final String circuitCode = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws Exception {
        service = new WorkflowService(moteur, historyRepository, eventPublisher, workflowRepository,
                validationInstanceRepository, stepFieldRepository, fieldValueRepository,
                transitionRepository, stepRepository);

        // Catalogue en mémoire : une étape « 10 » offrant une transition « 100 ».
        WorkflowPersistant aCircuit = new WorkflowPersistant(circuitCode);
        Etat aEtape = new Etat("10");
        aEtape.setLibelle("Vérification");
        aCircuit.addEtat(aEtape);
        when(moteur.getWorkflowByCode(circuitCode)).thenReturn(aCircuit);

        TransitionPersistante aTransition = new TransitionPersistante("100", aEtape, aEtape);
        aTransition.setLibelle("Valider");
        aTransition.setPermission("RESPONSABLE");
        when(moteur.getTransitionsPossibles(any()))
                .thenReturn(java.util.Collections.unmodifiableSequencedSet(
                        new LinkedHashSet<>(List.of(aTransition))));

        WorkflowTransition aTransitionBase = WorkflowTransition.builder()
                .id(100L).decision(StepDecision.APPROUVE).build();
        when(transitionRepository.findAllById(anyCollection())).thenReturn(List.of(aTransitionBase));

        WorkflowStep aEtapeBase = new WorkflowStep();
        aEtapeBase.setId(10L);
        aEtapeBase.setNomEtape("Vérification");
        aEtapeBase.setFields(new ArrayList<>(List.of(
                WorkflowStepField.builder().id(1L).fieldName("avis").fieldLabel("Avis").build())));
        when(stepRepository.findAvecChampsByIdIn(anyCollection())).thenReturn(List.of(aEtapeBase));
    }

    private WorkflowValidationInstance instance(UUID resourceId) {
        return WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(resourceId.toString())
                .resourceType("DOCUMENT")
                .workflowCode(circuitCode)
                .etatCode("10")
                .status(ValidationStatus.EN_COURS)
                .startedAt(LocalDateTime.now())
                .build();
    }

    // ------------------------------------------------------------------ coût du lot

    @Test
    @DisplayName("La lecture par lot coûte le même nombre de requêtes quel que soit le nombre de ressources")
    void lot_coutConstantEnRequetes() {
        List<UUID> aRessources = new ArrayList<>();
        List<WorkflowValidationInstance> aInstances = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            UUID aRessource = UUID.randomUUID();
            aRessources.add(aRessource);
            aInstances.add(instance(aRessource));
        }
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(aInstances);

        Map<UUID, WorkflowStateDto> aEtats = service.getWorkflowStatesForResources(aRessources);

        assertThat(aEtats).hasSize(50);

        // Trois requêtes, pour cinquante dossiers : les instances, les décisions, les champs.
        verify(validationInstanceRepository, times(1)).findByResourceIdInOrderByStartedAtDesc(anyCollection());
        verify(transitionRepository, times(1)).findAllById(anyCollection());
        verify(stepRepository, times(1)).findAvecChampsByIdIn(anyCollection());

        // Et surtout aucune lecture unitaire : c'est elle qui rendait le coût proportionnel.
        verify(validationInstanceRepository, never()).findTopByResourceIdOrderByStartedAtDesc(any());
        verify(validationInstanceRepository, never()).findById(any());
        verify(transitionRepository, never()).findById(any());
        verify(stepRepository, never()).findById(any());
    }

    @Test
    @DisplayName("La lecture unitaire ne relit plus le dossier deux fois")
    void lectureUnitaire_uneSeuleLectureDuDossier() {
        UUID aRessource = UUID.randomUUID();
        when(validationInstanceRepository.findTopByResourceIdOrderByStartedAtDesc(aRessource.toString()))
                .thenReturn(java.util.Optional.of(instance(aRessource)));

        WorkflowStateDto aEtat = service.getWorkflowStateForResource(aRessource);

        assertThat(aEtat).isNotNull();
        verify(validationInstanceRepository, times(1)).findTopByResourceIdOrderByStartedAtDesc(any());
        // getTransitionsPossibles(UUID) rechargeait le dossier que l'appelant venait de lire.
        verify(validationInstanceRepository, never()).findById(any());
    }

    // ------------------------------------------------------------------ contenu

    @Test
    @DisplayName("Le lot rend le même contenu que la lecture unitaire")
    void lot_memeContenuQueLunitaire() {
        UUID aRessource = UUID.randomUUID();
        WorkflowValidationInstance aInstance = instance(aRessource);
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(aInstance));
        when(validationInstanceRepository.findTopByResourceIdOrderByStartedAtDesc(aRessource.toString()))
                .thenReturn(java.util.Optional.of(aInstance));

        WorkflowStateDto aParLot = service.getWorkflowStatesForResources(List.of(aRessource)).get(aRessource);
        WorkflowStateDto aUnitaire = service.getWorkflowStateForResource(aRessource);

        assertThat(aParLot.getCurrentStateCode()).isEqualTo(aUnitaire.getCurrentStateCode());
        assertThat(aParLot.getCurrentStateName()).isEqualTo(aUnitaire.getCurrentStateName());
        assertThat(aParLot.getAllowedActions()).hasSameSizeAs(aUnitaire.getAllowedActions());
        assertThat(aParLot.getCurrentStepFields()).hasSameSizeAs(aUnitaire.getCurrentStepFields());
    }

    @Test
    @DisplayName("La décision et les champs de saisie sont bien restitués")
    void lot_decisionEtChampsRestitues() {
        UUID aRessource = UUID.randomUUID();
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(instance(aRessource)));

        WorkflowStateDto aEtat = service.getWorkflowStatesForResources(List.of(aRessource)).get(aRessource);

        assertThat(aEtat.getAllowedActions()).singleElement()
                .satisfies(action -> {
                    assertThat(action.getCode()).isEqualTo("100");
                    assertThat(action.getDecision()).isEqualTo("APPROUVE");
                    assertThat(action.getPermission()).isEqualTo("RESPONSABLE");
                });
        assertThat(aEtat.getCurrentStepFields()).singleElement()
                .satisfies(champ -> assertThat(champ.getFieldName()).isEqualTo("avis"));
    }

    @Test
    @DisplayName("Seule la dernière instance de chaque ressource est retenue")
    void lot_derniereInstanceSeulement() {
        UUID aRessource = UUID.randomUUID();
        WorkflowValidationInstance aRecente = instance(aRessource);
        WorkflowValidationInstance aAncienne = instance(aRessource);

        // La requête trie du plus récent au plus ancien : la première ligne fait foi.
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(aRecente, aAncienne));

        Map<UUID, WorkflowStateDto> aEtats = service.getWorkflowStatesForResources(List.of(aRessource));

        assertThat(aEtats).hasSize(1);
        assertThat(aEtats.get(aRessource).getInstanceId()).isEqualTo(aRecente.getId());
    }

    // ------------------------------------------------------------------ robustesse

    @Test
    @DisplayName("Un dossier incohérent est écarté sans faire échouer toute la page")
    void lot_dossierIncoherentEcarte() throws Exception {
        UUID aValide = UUID.randomUUID();
        UUID aCasse = UUID.randomUUID();

        WorkflowValidationInstance aInstanceCassee = instance(aCasse);
        aInstanceCassee.setEtatCode("999"); // étape absente du circuit

        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(instance(aValide), aInstanceCassee));

        Map<UUID, WorkflowStateDto> aEtats = service.getWorkflowStatesForResources(List.of(aValide, aCasse));

        assertThat(aEtats).containsOnlyKeys(aValide);
    }

    @Test
    @DisplayName("Un lot trop volumineux est refusé plutôt que servi")
    void lot_tropVolumineux_refuse() {
        List<UUID> aTrop = new ArrayList<>();
        for (int i = 0; i <= WorkflowService.TAILLE_LOT_MAX; i++) {
            aTrop.add(UUID.randomUUID());
        }

        assertThatThrownBy(() -> service.getWorkflowStatesForResources(aTrop))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trop de ressources");

        verify(validationInstanceRepository, never()).findByResourceIdInOrderByStartedAtDesc(anyCollection());
    }

    @Test
    @DisplayName("Une liste vide ou nulle ne déclenche aucune requête")
    void lot_videOuNul_aucuneRequete() {
        assertThat(service.getWorkflowStatesForResources(null)).isEmpty();
        assertThat(service.getWorkflowStatesForResources(List.of())).isEmpty();

        verify(validationInstanceRepository, never()).findByResourceIdInOrderByStartedAtDesc(anyCollection());
    }

    @Test
    @DisplayName("Les doublons de la demande ne sont interrogés qu'une fois")
    void lot_doublonsDedupliques() {
        UUID aRessource = UUID.randomUUID();
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(instance(aRessource)));

        Map<UUID, WorkflowStateDto> aEtats =
                service.getWorkflowStatesForResources(List.of(aRessource, aRessource, aRessource));

        assertThat(aEtats).hasSize(1);

        @SuppressWarnings("unchecked")
        var aCles = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(validationInstanceRepository).findByResourceIdInOrderByStartedAtDesc(aCles.capture());
        assertThat(aCles.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("Une ressource sans circuit est simplement absente du résultat")
    void lot_ressourceSansCircuit_absente() {
        UUID aSansCircuit = UUID.randomUUID();
        when(validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of());

        assertThat(service.getWorkflowStatesForResources(List.of(aSansCircuit))).isEmpty();
    }
}
