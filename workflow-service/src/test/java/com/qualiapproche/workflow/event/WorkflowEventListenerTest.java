package com.qualiapproche.workflow.event;

import com.qualiapproche.common.messagerie.AdressesMessagerie;
import com.qualiapproche.common.messagerie.BoiteDEnvoi;
import com.qualiapproche.common.messagerie.RappelDuCircuit;
import com.qualiapproche.workflow.messagerie.MessageSortant;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.model.ValidationHistory;
import com.qualiapproche.workflow.model.WorkflowFieldValue;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
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
 *
 * <p>La notification est désormais un <b>rappel</b> déposé dans la boîte d'envoi du service, puis
 * publié sur le courtier, en lieu et place du registre et de ses webhooks HTTP. Ce que ces tests
 * vérifient ne change pas : l'identifiant obtenu avant commit est celui qui est publié après, et la
 * charge utile transmise au service métier est intacte.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowEventListenerTest {

    @Mock private WorkflowStepRepository stepRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowValidationInstanceRepository validationInstanceRepository;
    @Mock private ValidationHistoryRepository historyRepository;
    @Mock private WorkflowFieldValueRepository fieldValueRepository;
    @Mock private BoiteDEnvoi<MessageSortant> boiteDEnvoi;
    @Mock private NotificateurEtapeParEmail notificateurParEmail;
    @Mock private NotificateurEnBase notificateurEnBase;

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
        when(boiteDEnvoi.deposer(anyString(), anyString(), any())).thenReturn(notificationId);
    }

    /** Le rappel déposé dans la boîte d'envoi, et la file à laquelle il est adressé. */
    private RappelDuCircuit rappelDepose(String destinationAttendue) {
        ArgumentCaptor<Object> aRappel = ArgumentCaptor.forClass(Object.class);
        verify(boiteDEnvoi).deposer(eq(destinationAttendue), eq(AdressesMessagerie.TYPE_RAPPEL_CIRCUIT),
                aRappel.capture());
        assertThat(aRappel.getValue()).isInstanceOf(RappelDuCircuit.class);
        return (RappelDuCircuit) aRappel.getValue();
    }

    /** La charge transmise au service métier — celle que portait jadis la notification. */
    private Map<String, Object> chargeDeposee() {
        return rappelDepose(AdressesMessagerie.RAPPELS_SUPPORT).getContenu();
    }

    @Test
    @DisplayName("L'identifiant enregistré avant commit est remis après commit")
    void notificationEnregistree_remiseApresCommit() {
        instanceExistante();
        TransitionFranchieEvent aEvenement = evenement();

        listener.enregistrerNotification(aEvenement);
        assertThat(aEvenement.getNotificationId()).isEqualTo(notificationId);

        listener.remettreNotification(aEvenement);
        verify(boiteDEnvoi).publier(notificationId);
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

        verify(boiteDEnvoi, never()).publier(any());
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
        when(boiteDEnvoi.deposer(anyString(), anyString(), any()))
                .thenReturn(aPremiereNotif)
                .thenReturn(aSecondeNotif);

        TransitionFranchieEvent aPremiere = evenement();
        TransitionFranchieEvent aSeconde = evenement();
        listener.enregistrerNotification(aPremiere);
        listener.enregistrerNotification(aSeconde);
        listener.remettreNotification(aPremiere);
        listener.remettreNotification(aSeconde);

        // Un support unique par fil aurait écrasé la première : seule la dernière était remise
        // sans attendre le prochain passage de l'ordonnanceur.
        verify(boiteDEnvoi).publier(aPremiereNotif);
        verify(boiteDEnvoi).publier(aSecondeNotif);
    }

    @Test
    @DisplayName("Une étape introuvable sur un état non terminal n'annule pas la transition")
    void etapeIntrouvable_transitionPreservee() {
        instanceExistante(); // stepRepository ne connaît pas l'étape « 2 »
        TransitionFranchieEvent aEvenement = evenement();

        // Cette phase s'exécute avant le commit : toute exception qui en sort annule la décision
        // de l'utilisateur. Le retrait du préfixe « TERMINATED_ » s'appliquait ici à l'état « 2 ».
        listener.enregistrerNotification(aEvenement);

        assertThat(chargeDeposee())
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

        assertThat((Map<?, ?>) chargeDeposee().get("fields"))
                .isEqualTo(Map.of("docRejet", "NON_CONFORMITE/DSI/abc.pdf"));
    }

    @Test
    @DisplayName("Une décision sans saisie ne transmet pas de champs")
    void aucuneSaisie_champsVides() {
        instanceExistante();
        when(historyRepository.findTopByValidationInstanceOrderByDecisionDateDesc(any()))
                .thenReturn(Optional.empty());

        listener.enregistrerNotification(evenement());

        assertThat((Map<?, ?>) chargeDeposee().get("fields")).isEmpty();
    }

    @Test
    @DisplayName("Un événement portant sur une autre classe d'entité est ignoré")
    void autreTypeDEntite_ignore() {
        TransitionFranchieEvent aEvenement = new TransitionFranchieEvent(
                "com.exemple.AutreEntite", instanceId.toString(), "circuit", "42",
                "1", "2", "agent", null);

        listener.enregistrerNotification(aEvenement);

        assertThat(aEvenement.getNotificationId()).isNull();
        verify(boiteDEnvoi, never()).deposer(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Un état terminal produit une charge utile portant la décision finale")
    void etatTerminal_chargeUtileFinale() {
        WorkflowValidationInstance aInstance = WorkflowValidationInstance.builder()
                .id(instanceId).resourceId(UUID.randomUUID().toString()).resourceType("DOCUMENT")
                .etatCode("TERMINATED_APPROUVE").status(ValidationStatus.TERMINE).build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(aInstance));
        when(boiteDEnvoi.deposer(anyString(), anyString(), any())).thenReturn(notificationId);

        TransitionFranchieEvent aEvenement = new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(), "circuit", "42",
                "1", "TERMINATED_APPROUVE", "agent", null);

        listener.enregistrerNotification(aEvenement);

        Map<String, Object> aCharge = chargeDeposee();
        assertThat(aCharge).containsEntry("status", "APPROVED");
        assertThat(aCharge).containsEntry("statusName", "APPROUVE");
        // Aucune étape ne correspond à un état terminal synthétique.
        verify(stepRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Une fin de circuit par clôture publie CLOSED, et non REJECTED")
    void etatTerminalCloture_publieClosed() {
        WorkflowValidationInstance aInstance = WorkflowValidationInstance.builder()
                .id(instanceId).resourceId(UUID.randomUUID().toString()).resourceType("DOCUMENT")
                .etatCode("TERMINATED_CLOTURE").status(ValidationStatus.TERMINE).build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(aInstance));
        when(boiteDEnvoi.deposer(anyString(), anyString(), any())).thenReturn(notificationId);

        TransitionFranchieEvent aEvenement = new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(), "circuit", "42",
                "1", "TERMINATED_CLOTURE", "agent", null);

        listener.enregistrerNotification(aEvenement);

        Map<String, Object> aCharge = chargeDeposee();
        // Le rejet servait d'issue par défaut à tout terminal non approuvé : la clôture ne doit
        // pas en hériter.
        assertThat(aCharge).containsEntry("status", "CLOSED");
        assertThat(aCharge).containsEntry("statusName", "CLOTURE");
    }

    // ------------------------------------------------------------------ rappel déposé

    @Test
    @DisplayName("Le rappel déposé nomme le dossier et porte la charge, adressé à la file de son service")
    void rappelDepose_versLeServiceDuDossier() {
        String aRessource = UUID.randomUUID().toString();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(
                WorkflowValidationInstance.builder().id(instanceId).resourceId(aRessource)
                        .resourceType("NON_CONFORMITE").etatCode("2").status(ValidationStatus.EN_COURS).build()));
        when(stepRepository.findById(2L)).thenReturn(Optional.empty());
        when(boiteDEnvoi.deposer(anyString(), anyString(), any())).thenReturn(notificationId);

        TransitionFranchieEvent aEvenement = evenement();
        listener.enregistrerNotification(aEvenement);

        // Une non-conformité appartient à amelioration-service : le rappel part sur sa file, et
        // non sur celle des documents. Le webhook HTTP qu'il remplace visait la même adresse.
        RappelDuCircuit aRappel = rappelDepose(AdressesMessagerie.RAPPELS_AMELIORATION);
        assertThat(aRappel.getTypeRessource()).isEqualTo("NON_CONFORMITE");
        assertThat(aRappel.getRessourceId()).isEqualTo(UUID.fromString(aRessource));
        assertThat(aRappel.getContenu())
                .containsEntry("status", "EN_COURS")
                .containsEntry("comments", "avis favorable")
                .containsKeys("statusName", "decision", "fields", "timestamp");
        assertThat(aEvenement.getNotificationId()).isEqualTo(notificationId);
        // Rien n'est publié avant le commit : une transition annulée n'annoncerait rien.
        verify(boiteDEnvoi, never()).publier(any());
    }

    @Test
    @DisplayName("Type de dossier qu'aucun service ne suit : aucun rappel déposé, la transition passe")
    void typeSansServiceDestinataire_aucunDepot() {
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(
                WorkflowValidationInstance.builder().id(instanceId).resourceId(UUID.randomUUID().toString())
                        .resourceType("AUDIT").etatCode("2").status(ValidationStatus.EN_COURS).build()));
        when(stepRepository.findById(2L)).thenReturn(Optional.empty());

        TransitionFranchieEvent aEvenement = evenement();
        listener.enregistrerNotification(aEvenement);
        listener.remettreNotification(aEvenement);

        // Une file sans consommateur garderait ces messages indéfiniment : mieux vaut ne rien dire.
        verify(boiteDEnvoi, never()).deposer(anyString(), anyString(), any());
        verify(boiteDEnvoi, never()).publier(any());
        assertThat(aEvenement.getNotificationId()).isNull();
        // La phase suivante a tout de même de quoi composer le courriel d'étape.
        assertThat(aEvenement.getResourceType()).isEqualTo("AUDIT");
    }

    @Test
    @DisplayName("Le rappel porte l'auteur de la transition, pour que le service destinataire lui attribue sa mise à jour")
    void rappelPorteLAuteur() {
        instanceExistante();
        org.springframework.security.oauth2.jwt.Jwt jeton = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("t").header("alg", "none").subject("agent-qualite").claim("name", "Awa Ouédraogo").build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.TestingAuthenticationToken(jeton, null));
        try {
            listener.enregistrerNotification(evenement());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        RappelDuCircuit aRappel = rappelDepose(AdressesMessagerie.RAPPELS_SUPPORT);
        assertThat(aRappel.getAuteurId()).isEqualTo("agent-qualite");
        assertThat(aRappel.getAuteurNom()).isEqualTo("Awa Ouédraogo");
    }

    @Test
    @DisplayName("Un geste sans appelant (automatique) transmet l'identifiant seul, sans nom emprunté")
    void rappelSansAppelant() {
        instanceExistante();

        listener.enregistrerNotification(evenement());

        RappelDuCircuit aRappel = rappelDepose(AdressesMessagerie.RAPPELS_SUPPORT);
        assertThat(aRappel.getAuteurId()).isEqualTo("agent-qualite");
        assertThat(aRappel.getAuteurNom()).isNull();
    }
}
