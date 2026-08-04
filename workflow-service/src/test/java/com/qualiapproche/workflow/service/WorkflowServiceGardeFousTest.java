package com.qualiapproche.workflow.service;

import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.event.CatalogueWorkflowModifieEvent;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
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
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Garde-fous d'ouverture et de suppression d'un circuit.
 *
 * <p>Chacun des cas couverts ici passait sans erreur avant correction, en laissant derrière lui
 * une donnée incohérente : deux circuits concurrents sur un même dossier, ou des instances en
 * cours rattachées à un circuit disparu.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceGardeFousTest {

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
                transitionRepository, stepRepository);
    }

    private Workflow circuit(boolean actif, String resourceType, int nbEtapes) {
        Workflow aWorkflow = Workflow.builder()
                .nom("Validation standard")
                .resourceType(resourceType)
                .actif(actif)
                .build();
        aWorkflow.setId(workflowId);

        List<WorkflowStep> aEtapes = new ArrayList<>();
        for (long i = 1; i <= nbEtapes; i++) {
            WorkflowStep aEtape = new WorkflowStep();
            aEtape.setId(i);
            aEtape.setCode("ETAPE_" + i);
            aEtape.setNomEtape("Étape " + i);
            aEtape.setWorkflow(aWorkflow);
            aEtapes.add(aEtape);
        }
        aWorkflow.setSteps(aEtapes);
        return aWorkflow;
    }

    private WorkflowValidationInstance instanceEnCours(String workflowCode) {
        return WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(resourceId.toString())
                .resourceType("DOCUMENT")
                .workflowCode(workflowCode)
                .etatCode("1")
                .status(ValidationStatus.EN_COURS)
                .build();
    }

    // ------------------------------------------------------------------ ouverture

    @Test
    @DisplayName("Ouvrir deux fois le même circuit rend l'instance déjà en cours, sans la dupliquer")
    void ouvertureRejouee_memeCircuit_rendLInstanceExistante() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 2)));
        WorkflowValidationInstance aExistante = instanceEnCours(workflowId.toString());
        when(validationInstanceRepository
                .findTopByResourceIdAndStatusOrderByStartedAtDesc(resourceId.toString(), ValidationStatus.EN_COURS))
                .thenReturn(Optional.of(aExistante));

        WorkflowInstanceDto aDto = service.initiateWorkflow(resourceId, "DOCUMENT", workflowId);

        assertThat(aDto.getInstanceId()).isEqualTo(aExistante.getId());
        verify(validationInstanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ouvrir un autre circuit sur un dossier déjà engagé est refusé en 409")
    void ouverture_autreCircuitEnCours_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 2)));
        when(validationInstanceRepository
                .findTopByResourceIdAndStatusOrderByStartedAtDesc(resourceId.toString(), ValidationStatus.EN_COURS))
                .thenReturn(Optional.of(instanceEnCours(UUID.randomUUID().toString())));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà en cours")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(validationInstanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un circuit désactivé ne peut plus être ouvert")
    void ouverture_circuitDesactive_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(false, "DOCUMENT", 2)));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    @DisplayName("Un circuit sans étape est refusé plutôt que de produire une instance sans état")
    void ouverture_circuitSansEtape_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 0)));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aucune étape");
    }

    @Test
    @DisplayName("Un circuit prévu pour un autre type de ressource est refusé en 400")
    void ouverture_typeDeRessourceIncoherent_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "NON_CONFORMITE", 2)));

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Un circuit introuvable donne un 404 et non une erreur serveur")
    void ouverture_circuitIntrouvable_404() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initiateWorkflow(resourceId, "DOCUMENT", workflowId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ suppression

    @Test
    @DisplayName("Supprimer un circuit portant des dossiers en cours est refusé en 409")
    void suppression_dossiersEnCours_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 2)));
        when(validationInstanceRepository.existsByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.deleteWorkflow(workflowId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en cours")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(workflowRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Supprimer un circuit sans dossier en cours reste possible")
    void suppression_sansDossierEnCours_autorisee() {
        Workflow aCircuit = circuit(true, "DOCUMENT", 2);
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(aCircuit));
        when(validationInstanceRepository.existsByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(false);

        service.deleteWorkflow(workflowId);

        verify(workflowRepository).delete(aCircuit);
    }

    @Test
    @DisplayName("Le rechargement du catalogue est différé après commit, et non joué dans la transaction")
    void modificationDuCatalogue_rechargementDiffereApresCommit() throws Exception {
        Workflow aCircuit = circuit(true, "DOCUMENT", 2);
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(aCircuit));
        when(validationInstanceRepository.existsByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(false);

        service.deleteWorkflow(workflowId);

        // Recharger dans la transaction exposait le moteur à des données non committées : une
        // annulation ultérieure lui laissait un catalogue décrivant des circuits inexistants.
        verify(moteur, never()).init();
        verify(eventPublisher).publishEvent(any(CatalogueWorkflowModifieEvent.class));
    }

    @Test
    @DisplayName("Supprimer un circuit introuvable donne un 404")
    void suppression_circuitIntrouvable_404() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteWorkflow(workflowId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Aucun workflow actif pour un type donne un 404 explicite")
    void workflowActif_absent_404() {
        when(workflowRepository.findByResourceTypeAndActifTrue(anyString())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getActiveWorkflowByType("DOCUMENT"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ configuration

    private WorkflowDto dtoCircuit(String resourceType) {
        return WorkflowDto.builder()
                .nom("wok")
                .resourceType(resourceType)
                .steps(new ArrayList<>(List.of(
                        WorkflowStepDto.builder().nomEtape("Rédaction").stepOrder(1).build())))
                .build();
    }

    @Test
    @DisplayName("Créer un circuit sur un code de type documentaire est refusé à la configuration")
    void creation_typeDeRessourceInconnu_refusee() {
        // 'PRO' est un type de document, pas une famille de ressource : le circuit était accepté,
        // puis rejeté à l'ouverture — après le dépôt du fichier, et pour l'utilisateur suivant.
        assertThatThrownBy(() -> service.createWorkflow(dtoCircuit("PRO")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DOCUMENT")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("La casse et les espaces du type de ressource sont normalisés, pas refusés")
    void creation_typeDeRessourceNormalise() {
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(i -> i.getArgument(0));

        WorkflowDto aCree = service.createWorkflow(dtoCircuit("  document "));

        assertThat(aCree.getResourceType()).isEqualTo("DOCUMENT");
    }

    @Test
    @DisplayName("Un type de ressource absent est refusé plutôt que persisté vide")
    void creation_typeDeRessourceAbsent_refusee() {
        assertThatThrownBy(() -> service.createWorkflow(dtoCircuit(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modifier un circuit vers un type de ressource inconnu est refusé de même")
    void modification_typeDeRessourceInconnu_refusee() {
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(circuit(true, "DOCUMENT", 1)));

        assertThatThrownBy(() -> service.updateWorkflow(workflowId, dtoCircuit("PRO")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(workflowRepository, never()).save(any());
    }
}
