package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.event.TransitionFranchieEvent;
import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.dto.WorkflowTransitionDto;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.model.ValidationHistory;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Retirer une étape d'un circuit qui porte des dossiers.
 *
 * <p>C'était refusé. Le refus se lisait sur les instances du moteur et non sur la table du module :
 * un dossier supprimé chez lui sans que le moteur en soit averti laissait une instance « en cours »
 * qui interdisait toute modification du circuit, pour toujours, sans dire de quel dossier il
 * s'agissait ni qu'il n'existait plus. L'administrateur ne pouvait ni corriger son circuit, ni
 * comprendre ce qui l'en empêchait.</p>
 *
 * <p>La suppression passe donc. Reste à ne pas laisser les dossiers sur une étape qui n'existe
 * plus — ils y deviendraient indécidables, ce que le refus cherchait justement à éviter : chacun
 * est ramené à la dernière étape conservée qui précède, et son historique en garde la trace.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuppressionDEtapeOccupeeTest {

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

    private final UUID circuitId = UUID.randomUUID();
    private final UUID dossierId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkflowService(moteur, historyRepository, eventPublisher, workflowRepository,
                validationInstanceRepository, stepFieldRepository, fieldValueRepository,
                transitionRepository, stepRepository,
                org.mockito.Mockito.mock(StructureUtilisateurService.class), null);
    }

    /** Trois étapes enregistrées : Réception (1), Traitement (2), Validation (3). */
    private Workflow circuitDeTroisEtapes() {
        Workflow circuit = Workflow.builder().nom("Circuit NC").resourceType("NON_CONFORMITE")
                .actif(true).build();
        circuit.setId(circuitId);
        List<WorkflowStep> etapes = new ArrayList<>();
        etapes.add(etape(1L, "RECEPTION", "Réception", 1, circuit));
        etapes.add(etape(2L, "TRAITEMENT", "Traitement", 2, circuit));
        etapes.add(etape(3L, "VALIDATION", "Validation", 3, circuit));
        circuit.setSteps(etapes);
        when(workflowRepository.findById(circuitId)).thenReturn(Optional.of(circuit));
        when(workflowRepository.findByResourceType("NON_CONFORMITE")).thenReturn(List.of(circuit));
        return circuit;
    }

    private WorkflowStep etape(Long id, String code, String nom, int ordre, Workflow circuit) {
        WorkflowStep etape = new WorkflowStep();
        etape.setId(id);
        etape.setCode(code);
        etape.setNomEtape(nom);
        etape.setStepOrder(ordre);
        etape.setWorkflow(circuit);
        return etape;
    }

    /** Ce que l'éditeur renvoie : le circuit amputé de l'étape retirée. */
    private WorkflowDto circuitSansLEtape(Long retiree) {
        List<WorkflowStepDto> etapes = new ArrayList<>();
        for (WorkflowStep etape : circuitDeReference()) {
            if (etape.getId().equals(retiree)) {
                continue;
            }
            etapes.add(WorkflowStepDto.builder()
                    .id(etape.getId()).code(etape.getCode()).nomEtape(etape.getNomEtape())
                    .stepOrder(etape.getStepOrder()).transitions(List.of()).fields(List.of())
                    .build());
        }
        return WorkflowDto.builder().id(circuitId).nom("Circuit NC")
                .resourceType("NON_CONFORMITE").actif(true).steps(etapes).build();
    }

    private List<WorkflowStep> circuitDeReference() {
        Workflow modele = Workflow.builder().build();
        return List.of(etape(1L, "RECEPTION", "Réception", 1, modele),
                etape(2L, "TRAITEMENT", "Traitement", 2, modele),
                etape(3L, "VALIDATION", "Validation", 3, modele));
    }

    private WorkflowValidationInstance dossierSurLEtape(String etatCode) {
        return WorkflowValidationInstance.builder()
                .id(dossierId)
                .resourceId(UUID.randomUUID().toString())
                .resourceType("NON_CONFORMITE")
                .workflowCode(circuitId.toString())
                .etatCode(etatCode)
                .faits("PLANS_ACTION_COMPLETS")
                .titulaireId("agent-impute")
                .status(ValidationStatus.EN_COURS)
                .build();
    }

    /**
     * Le même circuit, mais où la validation renvoie au traitement — le cas ordinaire : presque
     * toute étape d'un circuit est la destination d'une autre.
     */
    private Workflow circuitAvecRenvoiVersLeTraitement() {
        Workflow circuit = circuitDeTroisEtapes();
        WorkflowStep validation = circuit.getSteps().get(2);
        WorkflowStep traitement = circuit.getSteps().get(1);

        WorkflowTransition renvoi = new WorkflowTransition();
        renvoi.setId(10L);
        renvoi.setCode("REJETE");
        renvoi.setDecision(StepDecision.REJETE);
        renvoi.setLabel("Retourner pour retraitement");
        renvoi.setFromStep(validation);
        renvoi.setToStep(traitement);
        validation.getTransitions().add(renvoi);
        return circuit;
    }

    /** Ce que l'éditeur renvoie vraiment : l'étape en moins, l'action qui y menait toujours là. */
    private WorkflowDto circuitSansLeTraitementMaisAvecLeRenvoi() {
        WorkflowDto dto = circuitSansLEtape(2L);
        for (WorkflowStepDto etape : dto.getSteps()) {
            if ("VALIDATION".equals(etape.getCode())) {
                etape.setTransitions(List.of(WorkflowTransitionDto.builder()
                        .id(10L).code("REJETE").decision("REJETE")
                        .label("Retourner pour retraitement")
                        .toStepCode("TRAITEMENT")
                        .build()));
            }
        }
        return dto;
    }

    @Test
    @DisplayName("Une action qui menait à l'étape retirée n'empêche plus de la retirer")
    void actionVersLEtapeRetiree_neBloquePlus() {
        Workflow circuit = circuitAvecRenvoiVersLeTraitement();

        // Refusé jusqu'ici : « aucune étape de ce circuit ne porte le code TRAITEMENT », ou une
        // violation de contrainte sur to_step_id — selon ce que l'éditeur renvoyait.
        service.updateWorkflow(circuitId, circuitSansLeTraitementMaisAvecLeRenvoi());

        assertThat(circuit.getSteps()).extracting(WorkflowStep::getCode)
                .containsExactly("RECEPTION", "VALIDATION");
    }

    @Test
    @DisplayName("L'action qui y menait est supprimée, ni redirigée ni rendue terminale")
    void actionVersLEtapeRetiree_supprimee() {
        Workflow circuit = circuitAvecRenvoiVersLeTraitement();

        service.updateWorkflow(circuitId, circuitSansLeTraitementMaisAvecLeRenvoi());

        WorkflowStep validation = circuit.getSteps().stream()
                .filter(etape -> "VALIDATION".equals(etape.getCode())).findFirst().orElseThrow();
        // Rendue terminale, « retourner pour retraitement » serait devenue « clore le dossier ».
        assertThat(validation.getTransitions()).isEmpty();
    }

    @Test
    @DisplayName("Les actions vers les étapes conservées ne sont pas touchées")
    void actionVersUneEtapeConservee_intacte() {
        Workflow circuit = circuitAvecRenvoiVersLeTraitement();
        WorkflowDto dto = circuitSansLEtape(null);
        for (WorkflowStepDto etape : dto.getSteps()) {
            if ("VALIDATION".equals(etape.getCode())) {
                etape.setTransitions(List.of(WorkflowTransitionDto.builder()
                        .id(10L).code("REJETE").decision("REJETE")
                        .label("Retourner pour retraitement")
                        .toStepCode("TRAITEMENT")
                        .build()));
            }
        }

        service.updateWorkflow(circuitId, dto);

        WorkflowStep validation = circuit.getSteps().stream()
                .filter(etape -> "VALIDATION".equals(etape.getCode())).findFirst().orElseThrow();
        assertThat(validation.getTransitions()).hasSize(1);
        assertThat(validation.getTransitions().get(0).getToStep().getCode()).isEqualTo("TRAITEMENT");
    }

    @Test
    @DisplayName("Retirer une étape occupée n'est plus refusé")
    void etapeOccupee_suppressionAcceptee() {
        circuitDeTroisEtapes();
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of(dossierSurLEtape("2")));

        service.updateWorkflow(circuitId, circuitSansLEtape(2L));

        verify(workflowRepository).save(any(Workflow.class));
    }

    @Test
    @DisplayName("Le dossier est ramené à la dernière étape conservée qui précède")
    void dossier_replaceSurLEtapePrecedente() {
        circuitDeTroisEtapes();
        WorkflowValidationInstance dossier = dossierSurLEtape("2");
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of(dossier));

        service.updateWorkflow(circuitId, circuitSansLEtape(2L));

        // Réception, et non Validation : avancer un dossier reviendrait à tenir pour acquise une
        // décision que personne n'a prise.
        assertThat(dossier.getEtatCode()).isEqualTo("1");
        verify(validationInstanceRepository).save(dossier);
    }

    @Test
    @DisplayName("Aucune étape conservée avant celle qu'on retire : le dossier va à la première")
    void premiereEtapeRetiree_dossierVaALaPremiereConservee() {
        circuitDeTroisEtapes();
        WorkflowValidationInstance dossier = dossierSurLEtape("1");
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of(dossier));

        service.updateWorkflow(circuitId, circuitSansLEtape(1L));

        assertThat(dossier.getEtatCode()).isEqualTo("2");
    }

    @Test
    @DisplayName("Le replacement est inscrit à l'historique du dossier")
    void replacement_inscritALHistorique() {
        circuitDeTroisEtapes();
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of(dossierSurLEtape("2")));

        service.updateWorkflow(circuitId, circuitSansLEtape(2L));

        ArgumentCaptor<ValidationHistory> ligne = ArgumentCaptor.forClass(ValidationHistory.class);
        verify(historyRepository).save(ligne.capture());
        assertThat(ligne.getValue().getStepName()).isEqualTo("Traitement");
        assertThat(ligne.getValue().getDecision()).isEqualTo("Étape retirée du circuit");
        assertThat(ligne.getValue().getComments()).contains("Traitement").contains("Réception");
    }

    @Test
    @DisplayName("Les faits et le titulaire du dossier survivent au replacement")
    void replacement_conserveLesFaitsEtLeTitulaire() {
        circuitDeTroisEtapes();
        WorkflowValidationInstance dossier = dossierSurLEtape("2");
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of(dossier));

        service.updateWorkflow(circuitId, circuitSansLEtape(2L));

        // Ils ne dépendent pas de l'étape : les effacer ferait redemander ce qui est établi.
        assertThat(dossier.getFaits()).isEqualTo("PLANS_ACTION_COMPLETS");
        assertThat(dossier.getTitulaireId()).isEqualTo("agent-impute");
    }

    @Test
    @DisplayName("Le replacement est annoncé comme un changement d'étape ordinaire")
    void replacement_annonceAuModule() {
        circuitDeTroisEtapes();
        WorkflowValidationInstance dossier = dossierSurLEtape("2");
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of(dossier));

        service.updateWorkflow(circuitId, circuitSansLEtape(2L));

        // ApplicationEvent et non Object : publishEvent est surchargé, et c'est la surcharge
        // typée qui reçoit l'appel — un captor d'Object viserait l'autre, jamais appelée.
        ArgumentCaptor<org.springframework.context.ApplicationEvent> evenements =
                ArgumentCaptor.forClass(org.springframework.context.ApplicationEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publishEvent(evenements.capture());

        TransitionFranchieEvent annonce = evenements.getAllValues().stream()
                .filter(TransitionFranchieEvent.class::isInstance)
                .map(TransitionFranchieEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun changement d'étape annoncé."));

        assertThat(annonce.getEntityId()).isEqualTo(dossier.getId().toString());
        assertThat(annonce.getEtatAvant()).isEqualTo("2");
        assertThat(annonce.getEtatApres()).isEqualTo("1");
        // Aucune transition : le dossier n'a pas été décidé, il a été déplacé.
        assertThat(annonce.getTransitionCode()).isNull();
        assertThat(annonce.getCommentaire()).contains("Traitement").contains("Réception");
    }

    @Test
    @DisplayName("Une étape retirée que personne n'occupe n'annonce rien")
    void etapeLibre_aucuneAnnonce() {
        circuitDeTroisEtapes();
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of());

        service.updateWorkflow(circuitId, circuitSansLEtape(2L));

        verify(eventPublisher, never()).publishEvent(any(TransitionFranchieEvent.class));
    }

    @Test
    @DisplayName("Une modification qui ne retire aucune étape ne touche à aucun dossier")
    void aucuneEtapeRetiree_aucunDossierTouche() {
        circuitDeTroisEtapes();

        service.updateWorkflow(circuitId, circuitSansLEtape(null));

        verify(validationInstanceRepository, never()).findByEtatCodeInAndStatus(anyList(), any());
        verify(historyRepository, never()).save(any(ValidationHistory.class));
    }

    @Test
    @DisplayName("Une étape retirée que personne n'occupe ne produit aucune trace")
    void etapeLibre_aucuneTrace() {
        circuitDeTroisEtapes();
        when(validationInstanceRepository.findByEtatCodeInAndStatus(anyList(), any()))
                .thenReturn(List.of());

        service.updateWorkflow(circuitId, circuitSansLEtape(2L));

        verify(historyRepository, never()).save(any(ValidationHistory.class));
        verify(validationInstanceRepository, never()).save(any(WorkflowValidationInstance.class));
    }
}
