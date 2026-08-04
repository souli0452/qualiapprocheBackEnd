package com.qualiapproche.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowNotification.NotificationStatut;
import com.qualiapproche.workflow.repository.WorkflowNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Remise des notifications de transition (patron « outbox »).
 *
 * <p>L'enjeu couvert ici est l'unicité de la remise : le service métier enregistre la décision
 * qu'on lui poste, et la poster deux fois la dédouble.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowNotificationServiceTest {

    @Mock private WorkflowNotificationRepository notificationRepository;
    @Mock private SupportWebhookClient supportWebhookClient;
    @Mock private AmeliorationWebhookClient ameliorationWebhookClient;
    @Mock private SmtpEmailService emailService;

    private WorkflowNotificationService service;

    private final UUID notificationId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkflowNotificationService(notificationRepository, supportWebhookClient,
                emailService, ameliorationWebhookClient, new ObjectMapper());
        // Le service s'appelle lui-même à travers son proxy pour séparer les deux transactions ;
        // hors conteneur, il se suffit à lui-même.
        ReflectionTestUtils.setField(service, "self", service);
    }

    private WorkflowNotification notification(NotificationStatut statut, int tentatives) {
        return WorkflowNotification.builder()
                .id(notificationId)
                .resourceId(resourceId.toString())
                .resourceType("DOCUMENT")
                .payload("{\"status\":\"EN_COURS\",\"decision\":\"12\",\"statusName\":\"Vérification\"}")
                .statut(statut)
                .tentatives(tentatives)
                .build();
    }

    private void revendicationAcquise() {
        when(notificationRepository.revendiquer(eq(notificationId), any(), anyCollection(), any(), any()))
                .thenReturn(1);
    }

    private void revendicationPerdue() {
        when(notificationRepository.revendiquer(eq(notificationId), any(), anyCollection(), any(), any()))
                .thenReturn(0);
    }

    // ------------------------------------------------------------------ unicité de la remise

    @Test
    @DisplayName("Une notification déjà revendiquée par un autre ouvrier n'est pas postée une seconde fois")
    void revendicationPerdue_aucunAppelAuServiceMetier() {
        revendicationPerdue();

        boolean aRemise = service.remettre(notificationId);

        assertThat(aRemise).isFalse();
        verify(supportWebhookClient, never()).updateDocumentStatus(any(), any());
        verify(notificationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("La revendication précède tout appel réseau")
    void revendication_avantToutAppelReseau() {
        revendicationPerdue();

        service.remettre(notificationId);

        // Le seul échange avec la base doit être la revendication : rien n'est chargé, donc rien
        // n'est posté, tant que la prise en charge n'est pas acquise.
        verify(notificationRepository).revendiquer(eq(notificationId), any(), anyCollection(), any(), any());
        verify(supportWebhookClient, never()).updateDocumentStatus(any(), any());
    }

    @Test
    @DisplayName("La revendication ne porte que sur les statuts reprenables, jamais sur une remise acquise")
    void revendication_statutsReprenablesSeulement() {
        revendicationAcquise();
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification(NotificationStatut.EN_COURS_DE_REMISE, 1)));

        service.remettre(notificationId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<NotificationStatut>> aStatuts =
                ArgumentCaptor.forClass(Collection.class);
        verify(notificationRepository).revendiquer(eq(notificationId), eq(NotificationStatut.EN_COURS_DE_REMISE),
                aStatuts.capture(), any(), any());

        assertThat(aStatuts.getValue())
                .containsExactlyInAnyOrder(NotificationStatut.A_REMETTRE, NotificationStatut.EN_COURS_DE_REMISE)
                .doesNotContain(NotificationStatut.REMISE, NotificationStatut.ABANDONNEE);
    }

    // ------------------------------------------------------------------ issue de la remise

    @Test
    @DisplayName("Une remise réussie fige le statut et efface la dernière erreur")
    void remiseReussie_statutRemise() {
        revendicationAcquise();
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification(NotificationStatut.EN_COURS_DE_REMISE, 1)));

        boolean aRemise = service.remettre(notificationId);

        assertThat(aRemise).isTrue();
        verify(supportWebhookClient).updateDocumentStatus(eq(resourceId), any());

        ArgumentCaptor<WorkflowNotification> aSauvegardee = ArgumentCaptor.forClass(WorkflowNotification.class);
        verify(notificationRepository).save(aSauvegardee.capture());
        assertThat(aSauvegardee.getValue().getStatut()).isEqualTo(NotificationStatut.REMISE);
        assertThat(aSauvegardee.getValue().getRemiseAt()).isNotNull();
        assertThat(aSauvegardee.getValue().getDerniereErreur()).isNull();
    }

    @Test
    @DisplayName("Un échec rend la notification reprenable, avec report — sans réincrémenter le compteur")
    void remiseEnEchec_rendueReprenableAvecReport() {
        revendicationAcquise();
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification(NotificationStatut.EN_COURS_DE_REMISE, 2)));
        org.mockito.Mockito.doThrow(new IllegalStateException("service indisponible"))
                .when(supportWebhookClient).updateDocumentStatus(any(), any());

        boolean aRemise = service.remettre(notificationId);

        assertThat(aRemise).isFalse();
        ArgumentCaptor<WorkflowNotification> aSauvegardee = ArgumentCaptor.forClass(WorkflowNotification.class);
        verify(notificationRepository).save(aSauvegardee.capture());

        WorkflowNotification aResultat = aSauvegardee.getValue();
        assertThat(aResultat.getStatut())
                .as("laissée « en cours de remise », elle n'aurait été reprise qu'à l'expiration "
                        + "de la revendication")
                .isEqualTo(NotificationStatut.A_REMETTRE);
        assertThat(aResultat.getTentatives())
                .as("le compteur est incrémenté par la revendication, pas une seconde fois ici")
                .isEqualTo(2);
        assertThat(aResultat.getProchaineTentativeAt()).isAfter(LocalDateTime.now());
        assertThat(aResultat.getDerniereErreur()).contains("service indisponible");
    }

    @Test
    @DisplayName("Le nombre maximal de tentatives atteint fait passer la notification en abandon")
    void tentativesEpuisees_abandon() {
        revendicationAcquise();
        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.of(notification(NotificationStatut.EN_COURS_DE_REMISE,
                        WorkflowNotificationService.TENTATIVES_MAX)));
        org.mockito.Mockito.doThrow(new IllegalStateException("toujours indisponible"))
                .when(supportWebhookClient).updateDocumentStatus(any(), any());

        service.remettre(notificationId);

        ArgumentCaptor<WorkflowNotification> aSauvegardee = ArgumentCaptor.forClass(WorkflowNotification.class);
        verify(notificationRepository).save(aSauvegardee.capture());
        assertThat(aSauvegardee.getValue().getStatut()).isEqualTo(NotificationStatut.ABANDONNEE);
    }

    @Test
    @DisplayName("Un type de ressource sans destinataire n'est pas rejoué indéfiniment")
    void typeDeRessourceInconnu_echecExplicite() {
        revendicationAcquise();
        WorkflowNotification aNotification = notification(NotificationStatut.EN_COURS_DE_REMISE, 1);
        aNotification.setResourceType("TYPE_INCONNU");
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(aNotification));

        boolean aRemise = service.remettre(notificationId);

        assertThat(aRemise).isFalse();
        verify(supportWebhookClient, never()).updateDocumentStatus(any(), any());
        verify(ameliorationWebhookClient, never()).updateNonConformiteStatus(any(), any());
    }

    // ------------------------------------------------------------------ reprise

    @Test
    @DisplayName("La reprise couvre aussi les revendications restées sans suite")
    void reprise_inclutLesRevendicationsExpirees() {
        when(notificationRepository.aReprendre(anyCollection(), any(), any())).thenReturn(List.of());

        service.notificationsARejouer(50);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<NotificationStatut>> aStatuts = ArgumentCaptor.forClass(Collection.class);
        verify(notificationRepository).aReprendre(aStatuts.capture(), any(), any());

        assertThat(aStatuts.getValue())
                .as("sans le statut « en cours de remise », une notification revendiquée par un pod "
                        + "disparu resterait indéfiniment en suspens")
                .contains(NotificationStatut.EN_COURS_DE_REMISE, NotificationStatut.A_REMETTRE);
    }

    @Test
    @DisplayName("L'enregistrement se fait en attente de remise, dans la transaction de la transition")
    void enregistrement_statutInitial() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.enregistrer(resourceId.toString(), "DOCUMENT", Map.of("status", "EN_COURS"));

        ArgumentCaptor<WorkflowNotification> aSauvegardee = ArgumentCaptor.forClass(WorkflowNotification.class);
        verify(notificationRepository).save(aSauvegardee.capture());
        assertThat(aSauvegardee.getValue().getStatut()).isEqualTo(NotificationStatut.A_REMETTRE);
        assertThat(aSauvegardee.getValue().getPayload()).contains("EN_COURS");
    }
}
