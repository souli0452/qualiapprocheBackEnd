package com.qualiapproche.workflow.event;

import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.model.ValidationHistory;
import com.qualiapproche.workflow.model.WorkflowFieldValue;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.SmtpEmailService;
import com.qualiapproche.workflow.service.WorkflowNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Propagation de la notification entre les deux phases d'écoute d'une transition.
 *
 * <p>L'identifiant transitait par un {@code ThreadLocal}. Ces tests fixent le comportement qui
 * faisait défaut : une transaction annulée n'atteignant jamais la phase d'après commit, la valeur
 * restait accrochée au fil de pool et la requête suivante remettait une notification qui n'était
 * pas la sienne.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowEventListenerTest {

    @Mock private WorkflowStepRepository stepRepository;
    @Mock private EmailTemplateRepository emailTemplateRepository;
    @Mock private SmtpEmailService emailService;
    @Mock private WorkflowValidationInstanceRepository validationInstanceRepository;
    @Mock private ValidationHistoryRepository historyRepository;
    @Mock private WorkflowFieldValueRepository fieldValueRepository;
    @Mock private WorkflowNotificationService notificationService;

    @InjectMocks private WorkflowEventListener listener;

    private final UUID instanceId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();

    private TransitionFranchieEvent evenement() {
        return new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(),
                UUID.randomUUID().toString(), "42", "1", "2", "agent-qualite", "avis favorable");
    }

    private void instanceExistante() {
        WorkflowValidationInstance aInstance = WorkflowValidationInstance.builder()
                .id(instanceId)
                .resourceId(UUID.randomUUID().toString())
                .resourceType("DOCUMENT")
                .etatCode("2")
                .status(ValidationStatus.EN_COURS)
                .build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(aInstance));
        when(stepRepository.findById(2L)).thenReturn(Optional.empty());
        when(notificationService.enregistrer(anyString(), anyString(), any()))
                .thenReturn(WorkflowNotification.builder().id(notificationId).build());
    }

    @Test
    @DisplayName("L'identifiant enregistré avant commit est remis après commit")
    void notificationEnregistree_remiseApresCommit() {
        instanceExistante();
        TransitionFranchieEvent aEvenement = evenement();

        listener.enregistrerNotification(aEvenement);
        assertThat(aEvenement.getNotificationId()).isEqualTo(notificationId);

        listener.remettreNotification(aEvenement);
        verify(notificationService).remettre(notificationId);
    }

    @Test
    @DisplayName("Une transaction annulée ne laisse rien derrière elle : la transition suivante "
            + "sur le même fil ne remet pas la notification de la précédente")
    void transactionAnnulee_aucuneRemiseParasite() {
        instanceExistante();

        // Transaction 1 : la notification est enregistrée, puis le commit échoue — la phase
        // d'après commit n'a jamais lieu.
        TransitionFranchieEvent aPremiere = evenement();
        listener.enregistrerNotification(aPremiere);

        // Transaction 2, sur le même fil : son propre événement ne porte aucune notification.
        TransitionFranchieEvent aSeconde = evenement();
        listener.remettreNotification(aSeconde);

        verify(notificationService, never()).remettre(any());
    }

    @Test
    @DisplayName("Deux transitions dans une même transaction sont chacune remises")
    void deuxTransitions_chacuneRemise() {
        WorkflowValidationInstance aInstance = WorkflowValidationInstance.builder()
                .id(instanceId).resourceId(UUID.randomUUID().toString()).resourceType("DOCUMENT")
                .etatCode("2").status(ValidationStatus.EN_COURS).build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(aInstance));
        when(stepRepository.findById(2L)).thenReturn(Optional.empty());

        UUID aPremiereNotif = UUID.randomUUID();
        UUID aSecondeNotif = UUID.randomUUID();
        when(notificationService.enregistrer(anyString(), anyString(), any()))
                .thenReturn(WorkflowNotification.builder().id(aPremiereNotif).build())
                .thenReturn(WorkflowNotification.builder().id(aSecondeNotif).build());

        TransitionFranchieEvent aPremiere = evenement();
        TransitionFranchieEvent aSeconde = evenement();
        listener.enregistrerNotification(aPremiere);
        listener.enregistrerNotification(aSeconde);
        listener.remettreNotification(aPremiere);
        listener.remettreNotification(aSeconde);

        // Un support unique par fil aurait écrasé la première : seule la dernière était remise
        // sans attendre le prochain passage de l'ordonnanceur.
        verify(notificationService).remettre(aPremiereNotif);
        verify(notificationService).remettre(aSecondeNotif);
    }

    @Test
    @DisplayName("Une étape introuvable sur un état non terminal n'annule pas la transition")
    void etapeIntrouvable_transitionPreservee() {
        instanceExistante(); // stepRepository ne connaît pas l'étape « 2 »
        TransitionFranchieEvent aEvenement = evenement();

        // Cette phase s'exécute avant le commit : toute exception qui en sort annule la décision
        // de l'utilisateur. Le retrait du préfixe « TERMINATED_ » s'appliquait ici à l'état « 2 ».
        listener.enregistrerNotification(aEvenement);

        @SuppressWarnings("unchecked")
        var aCharge = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrer(anyString(), anyString(), aCharge.capture());
        assertThat(aCharge.getValue())
                .as("le circuit n'est pas terminé : il ne doit pas être annoncé comme tel")
                .containsEntry("status", "EN_COURS");
    }

    @Test
    @DisplayName("Les valeurs saisies à l'étape accompagnent la notification, indexées par nom de champ")
    void champsSaisis_transmisAuServiceMetier() {
        instanceExistante();
        ValidationHistory aHistorique = ValidationHistory.builder().id(7L).build();
        when(historyRepository.findTopByValidationInstanceOrderByDecisionDateDesc(any()))
                .thenReturn(Optional.of(aHistorique));
        when(fieldValueRepository.findByHistory_Id(7L)).thenReturn(java.util.List.of(
                WorkflowFieldValue.builder().fieldName("docRejet").value("NON_CONFORMITE/DSI/abc.pdf").build(),
                // Un champ sans valeur n'a rien à dire au module métier : le transmettre à vide
                // écraserait la donnée déjà en place.
                WorkflowFieldValue.builder().fieldName("observationRejet").value(null).build()));

        TransitionFranchieEvent aEvenement = evenement();
        listener.enregistrerNotification(aEvenement);

        @SuppressWarnings("unchecked")
        var aCharge = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrer(anyString(), anyString(), aCharge.capture());
        assertThat((Map<?, ?>) aCharge.getValue().get("fields"))
                .isEqualTo(Map.of("docRejet", "NON_CONFORMITE/DSI/abc.pdf"));
    }

    @Test
    @DisplayName("Une décision sans saisie ne transmet pas de champs")
    void aucuneSaisie_champsVides() {
        instanceExistante();
        when(historyRepository.findTopByValidationInstanceOrderByDecisionDateDesc(any()))
                .thenReturn(Optional.empty());

        listener.enregistrerNotification(evenement());

        @SuppressWarnings("unchecked")
        var aCharge = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrer(anyString(), anyString(), aCharge.capture());
        assertThat((Map<?, ?>) aCharge.getValue().get("fields")).isEmpty();
    }

    @Test
    @DisplayName("Un événement portant sur une autre classe d'entité est ignoré")
    void autreTypeDEntite_ignore() {
        TransitionFranchieEvent aEvenement = new TransitionFranchieEvent(
                "com.exemple.AutreEntite", instanceId.toString(), "circuit", "42",
                "1", "2", "agent", null);

        listener.enregistrerNotification(aEvenement);

        assertThat(aEvenement.getNotificationId()).isNull();
        verify(notificationService, never()).enregistrer(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Un état terminal produit une charge utile portant la décision finale")
    void etatTerminal_chargeUtileFinale() {
        WorkflowValidationInstance aInstance = WorkflowValidationInstance.builder()
                .id(instanceId).resourceId(UUID.randomUUID().toString()).resourceType("DOCUMENT")
                .etatCode("TERMINATED_APPROUVE").status(ValidationStatus.TERMINE).build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(aInstance));
        when(notificationService.enregistrer(anyString(), anyString(), any()))
                .thenReturn(WorkflowNotification.builder().id(notificationId).build());

        TransitionFranchieEvent aEvenement = new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(), "circuit", "42",
                "1", "TERMINATED_APPROUVE", "agent", null);

        listener.enregistrerNotification(aEvenement);

        @SuppressWarnings("unchecked")
        var aCharge = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrer(anyString(), anyString(), aCharge.capture());
        assertThat(aCharge.getValue()).containsEntry("status", "APPROVED");
        assertThat(aCharge.getValue()).containsEntry("statusName", "APPROUVE");
        // Aucune étape ne correspond à un état terminal synthétique.
        verify(stepRepository, never()).findById(any());
    }
}
