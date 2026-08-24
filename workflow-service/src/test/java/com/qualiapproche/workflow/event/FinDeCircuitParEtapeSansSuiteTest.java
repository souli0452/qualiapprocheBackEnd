package com.qualiapproche.workflow.event;

import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.WorkflowNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ce que le moteur annonce au module métier quand le dossier atteint une étape sans action.
 *
 * <p>Une telle étape est une fin de circuit voulue — c'est ainsi que sont écrits les circuits
 * livrés, l'étape « Clôture » d'une non-conformité ou « Soldée » d'un plan d'action. Le moteur
 * annonçait pourtant « en cours » : doublement faux, puisqu'il venait de clore l'instance et que
 * le module attendait dès lors une suite qui ne viendrait jamais. Un document restait ainsi hors
 * des documents en vigueur, et aucune demande de modification ne pouvait le viser.</p>
 *
 * <p>L'issue se lit sur la décision qui a mené à l'étape, comme pour une action terminale : un
 * circuit peut se terminer sur une étape « Refusé » comme sur une étape « Clôturé », et les
 * confondre ferait entrer en vigueur un document que l'on vient d'écarter.</p>
 */
class FinDeCircuitParEtapeSansSuiteTest {

    private static final long ETAPE_CLOTURE = 9L;
    private static final long ETAPE_INTERMEDIAIRE = 7L;
    private static final String TRANSITION = "42";

    private WorkflowStepRepository stepRepository;
    private WorkflowTransitionRepository transitionRepository;
    private WorkflowValidationInstanceRepository validationInstanceRepository;
    private WorkflowNotificationService notificationService;
    private WorkflowEventListener listener;

    private final UUID instanceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        stepRepository = mock(WorkflowStepRepository.class);
        transitionRepository = mock(WorkflowTransitionRepository.class);
        validationInstanceRepository = mock(WorkflowValidationInstanceRepository.class);
        ValidationHistoryRepository historyRepository = mock(ValidationHistoryRepository.class);
        WorkflowFieldValueRepository fieldValueRepository = mock(WorkflowFieldValueRepository.class);
        notificationService = mock(WorkflowNotificationService.class);

        listener = new WorkflowEventListener(stepRepository, transitionRepository,
                validationInstanceRepository, historyRepository, fieldValueRepository,
                notificationService, mock(NotificateurEtapeParEmail.class));

        when(historyRepository.findTopByValidationInstanceOrderByDecisionDateDesc(any()))
                .thenReturn(Optional.empty());
        when(notificationService.enregistrer(anyString(), anyString(), any()))
                .thenReturn(WorkflowNotification.builder().id(UUID.randomUUID()).build());
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(
                WorkflowValidationInstance.builder()
                        .id(instanceId)
                        .resourceId(UUID.randomUUID().toString())
                        .resourceType("DOCUMENT")
                        .status(ValidationStatus.EN_COURS)
                        .build()));
    }

    /** L'étape atteinte, et ce qu'elle offre encore comme suite. */
    private void etapeAtteinte(long id, String nom, String etatTraitement, boolean offreUneAction) {
        WorkflowStep step = new WorkflowStep();
        step.setId(id);
        step.setNomEtape(nom);
        step.setEtatTraitement(etatTraitement);
        when(stepRepository.findById(id)).thenReturn(Optional.of(step));
        when(transitionRepository.existsByFromStepId(id)).thenReturn(offreUneAction);
    }

    /** La décision par laquelle le dossier y est parvenu. */
    private void decisionFranchie(StepDecision decision) {
        WorkflowTransition transition = WorkflowTransition.builder().decision(decision).build();
        transition.setId(Long.valueOf(TRANSITION));
        when(transitionRepository.findById(Long.valueOf(TRANSITION))).thenReturn(Optional.of(transition));
    }

    private Map<String, Object> chargePubliee(long etapeAtteinte) {
        listener.enregistrerNotification(new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(),
                UUID.randomUUID().toString(), TRANSITION, "1", String.valueOf(etapeAtteinte),
                "agent-qualite", "avis favorable"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> charge = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrer(anyString(), anyString(), charge.capture());
        return charge.getValue();
    }

    @Test
    @DisplayName("Étape sans action atteinte par une clôture : la fin de circuit est annoncée close")
    void etapeSansSuite_parCloture_annonceCLOSED() {
        etapeAtteinte(ETAPE_CLOTURE, "Clôturer", "CLOTURE", false);
        decisionFranchie(StepDecision.CLOTURE);

        Map<String, Object> charge = chargePubliee(ETAPE_CLOTURE);

        assertThat(charge.get("status")).isEqualTo("CLOSED");
        // Le libellé de l'étape et son état de traitement restent publiés : c'est ce qui permet au
        // module métier de dire « clôturé » plutôt qu'un simple « terminé ».
        assertThat(charge.get("statusName")).isEqualTo("Clôturer");
        assertThat(charge.get("etatCode")).isEqualTo("CLOTURE");
    }

    @Test
    @DisplayName("Étape sans action atteinte par une approbation : le dossier est approuvé")
    void etapeSansSuite_parApprobation_annonceAPPROVED() {
        etapeAtteinte(ETAPE_CLOTURE, "Mise en vigueur", "CLOTURE", false);
        decisionFranchie(StepDecision.APPROUVE);

        assertThat(chargePubliee(ETAPE_CLOTURE).get("status")).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Étape sans action atteinte par un rejet : le dossier est refusé, non mis en vigueur")
    void etapeSansSuite_parRejet_annonceREJECTED() {
        etapeAtteinte(ETAPE_CLOTURE, "Refusé", "REJETE", false);
        decisionFranchie(StepDecision.REJETE);

        assertThat(chargePubliee(ETAPE_CLOTURE).get("status")).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Décision introuvable : la fin est annoncée close plutôt que refusée")
    void etapeSansSuite_decisionInconnue_annonceCLOSED() {
        etapeAtteinte(ETAPE_CLOTURE, "Clôturer", "CLOTURE", false);
        when(transitionRepository.findById(Long.valueOf(TRANSITION))).thenReturn(Optional.empty());

        assertThat(chargePubliee(ETAPE_CLOTURE).get("status")).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("Étape qui offre encore une action : le circuit reste en cours")
    void etapeAvecSuite_resteEnCours() {
        etapeAtteinte(ETAPE_INTERMEDIAIRE, "Vérification", "VERIFICATION", true);

        Map<String, Object> charge = chargePubliee(ETAPE_INTERMEDIAIRE);

        assertThat(charge.get("status")).isEqualTo("EN_COURS");
        assertThat(charge.get("statusName")).isEqualTo("Vérification");
    }
}
