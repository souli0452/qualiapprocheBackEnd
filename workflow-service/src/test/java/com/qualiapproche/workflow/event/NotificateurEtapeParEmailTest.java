package com.qualiapproche.workflow.event;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.DestinatairesEtapeService;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.service.WorkflowNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.scheduling.annotation.Async;

/**
 * Envoi du courriel d'étape à ses responsables réels.
 *
 * <p>Le destinataire était fabriqué en accolant le nom du rôle à un domaine
 * ({@code role_VERIFICATEUR@qualiapproche.com}) : une boîte inexistante, vers laquelle l'envoi
 * n'échouait pas. Aucune notification d'étape n'a jamais atteint quiconque, et rien ne le
 * signalait dans les journaux.</p>
 *
 * <p>Le courriel n'est plus envoyé ici : il est <b>enregistré</b> au registre des notifications,
 * qui le remet et le rejoue en cas d'échec. Ce que ces tests vérifient reste le même — la bonne
 * adresse, le bon contenu, et l'indépendance des destinataires entre eux — mais au point où la
 * remise est confiée, non exécutée.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificateurEtapeParEmailTest {

    @Mock private EmailTemplateRepository emailTemplateRepository;
    @Mock private WorkflowNotificationService notificationService;
    @Mock private DestinatairesEtapeService destinatairesEtapeService;
    // Requis depuis que chaque courriel lit la référence du dossier sur l'instance : sans ce mock,
    // l'injection laisse le champ nul et la variable {numeroNc} fait échouer tout enregistrement.
    @Mock private WorkflowValidationInstanceRepository validationInstanceRepository;
    // Résout la structure d'un dossier qui n'en porte pas encore, depuis son créateur. Les tests
    // le laissent muet — il rend null — sauf celui qui vérifie la réparation.
    @Mock private com.qualiapproche.workflow.service.StructureUtilisateurService structureUtilisateurService;
    // Réelle et vide plutôt que simulée : sans routes configurées, le lien est vide — le
    // comportement qu'attendent ces tests, écrits avant la table.
    @org.mockito.Spy private com.qualiapproche.workflow.config.LienVersLeDossier lienVersLeDossier =
            new com.qualiapproche.workflow.config.LienVersLeDossier();

    @InjectMocks private NotificateurEtapeParEmail notificateur;

    private WorkflowStep etape;

    @BeforeEach
    void setUp() {
        etape = new WorkflowStep();
        etape.setId(2L);
        etape.setNomEtape("Vérification");
        etape.setResponsableRole("VERIFICATEUR");
        etape.setEmailTemplateCode("NOTIF_ETAPE");

        when(emailTemplateRepository.findByCode("NOTIF_ETAPE")).thenReturn(Optional.of(
                EmailTemplate.builder()
                        .code("NOTIF_ETAPE")
                        .subject("Un dossier vous attend")
                        .body("<p>Bonjour {{user}}, le dossier {{entityId}} est à l'étape {{etatApres}}.</p>")
                        .build()));
    }

    private TransitionFranchieEvent evenement() {
        return new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), UUID.randomUUID().toString(),
                "circuit", "42", "1", "2", "agent-qualite", "avis favorable");
    }

    private void destinataires(DestinataireDto... destinataires) {
        when(destinatairesEtapeService.destinatairesDuRole(eq("VERIFICATEUR"), any()))
                .thenReturn(List.of(destinataires));
    }

    private DestinataireDto destinataire(String email, String nom) {
        return DestinataireDto.builder().userId(UUID.randomUUID().toString())
                .email(email).nomComplet(nom).build();
    }

    @Test
    @DisplayName("Chaque responsable de l'étape reçoit le courriel à sa propre adresse")
    void responsables_recoiventLeCourriel() {
        destinataires(destinataire("claire@exemple.fr", "Claire Martin"),
                destinataire("sam@exemple.fr", "Sam Diop"));

        notificateur.notifier(etape, evenement());

        ArgumentCaptor<String> aAdresses = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(2)).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                aAdresses.capture(), anyString(), anyString(), anyMap());

        assertThat(aAdresses.getAllValues())
                .containsExactly("claire@exemple.fr", "sam@exemple.fr")
                .as("l'adresse fabriquée à partir du rôle ne correspondait à aucune boîte")
                .noneMatch(adresse -> adresse.startsWith("role_"));
    }

    @Test
    @DisplayName("Le corps du message est personnalisé au nom de chaque destinataire")
    void corps_personnaliseParDestinataire() {
        destinataires(destinataire("claire@exemple.fr", "Claire Martin"));

        notificateur.notifier(etape, evenement());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> aVariables = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                anyString(), anyString(), anyString(), aVariables.capture());

        assertThat(aVariables.getValue()).containsEntry("user", "Claire Martin");
        assertThat(aVariables.getValue()).containsEntry("etatApres", "Vérification");
    }

    @Test
    @DisplayName("Un rôle que personne ne porte n'envoie rien, et le signale")
    void aucunResponsable_aucunEnvoi() {
        destinataires();

        notificateur.notifier(etape, evenement());

        verify(notificationService, never()).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("L'échec d'un enregistrement ne prive pas les autres destinataires")
    void echecDunEnvoi_lesAutresSontServis() {
        destinataires(destinataire("casse@exemple.fr", "Boîte Pleine"),
                destinataire("sam@exemple.fr", "Sam Diop"));
        doThrow(new IllegalStateException("registre indisponible"))
                .when(notificationService).enregistrerCourriel(
                        nullable(String.class), nullable(String.class),
                        eq("casse@exemple.fr"), anyString(), anyString(), anyMap());

        notificateur.notifier(etape, evenement());

        verify(notificationService).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                eq("sam@exemple.fr"), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("La remise est demandée au registre, qui la rejouera si elle échoue")
    void remiseConfieeAuRegistre() {
        destinataires(destinataire("claire@exemple.fr", "Claire Martin"));
        WorkflowNotification enregistree = WorkflowNotification.builder()
                .resourceId("42").resourceType("DOCUMENT").payload("{}")
                .statut(WorkflowNotification.NotificationStatut.A_REMETTRE)
                .build();
        enregistree.setId(UUID.randomUUID());
        when(notificationService.enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(enregistree);

        notificateur.notifier(etape, evenement());

        // Remise tentée aussitôt : sans cela, le courriel attendrait le prochain passage de
        // l'ordonnanceur alors que le destinataire a un dossier à traiter maintenant.
        verify(notificationService).remettre(enregistree.getId());
    }

    @Test
    @DisplayName("La structure du dossier borne la résolution : elle est transmise avec le rôle")
    void structureDuDossier_transmiseAvecLeRole() {
        UUID instanceId = UUID.randomUUID();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(
                WorkflowValidationInstance.builder()
                        .resourceId("42").resourceType("DOCUMENT")
                        .structureId("structure-7")
                        .build()));
        when(destinatairesEtapeService.destinatairesDuRole("VERIFICATEUR", "structure-7"))
                .thenReturn(List.of(destinataire("claire@exemple.fr", "Claire Martin")));

        TransitionFranchieEvent event = new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(),
                "circuit", "42", "1", "2", "agent-qualite", "avis favorable");
        notificateur.notifier(etape, event);

        // Sans la structure, le rôle seul était interrogé : les vérificateurs de toutes les
        // structures recevaient le courriel d'un dossier qui ne concernait qu'une seule d'entre elles.
        verify(destinatairesEtapeService).destinatairesDuRole("VERIFICATEUR", "structure-7");
    }

    @Test
    @DisplayName("Un dossier sans structure emprunte celle de son créateur, et la garde")
    void dossierSansStructure_repareDepuisLeCreateur() {
        UUID instanceId = UUID.randomUUID();
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .resourceId("42").resourceType("DOCUMENT")
                .createurId("createur-1")
                .build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(structureUtilisateurService.structureDe("createur-1")).thenReturn("structure-7");
        when(destinatairesEtapeService.destinatairesDuRole("VERIFICATEUR", "structure-7"))
                .thenReturn(List.of(destinataire("claire@exemple.fr", "Claire Martin")));

        notificateur.notifier(etape, new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(),
                "circuit", "42", "1", "2", "agent-qualite", "avis favorable"));

        // Dossiers ouverts avant la colonne, ou pendant que le jeton ne portait pas de structure :
        // sans cette réparation, leurs courriels repartaient vers toute la plateforme.
        verify(destinatairesEtapeService).destinatairesDuRole("VERIFICATEUR", "structure-7");
        assertThat(instance.getStructureId()).isEqualTo("structure-7");
        verify(validationInstanceRepository).save(instance);
    }

    @Test
    @DisplayName("Une étape peut annoncer son franchissement à la structure d'où vient le dossier")
    void destinataireDesigne_structureEmettrice() {
        // La clôture d'une non-conformité s'annonce au pilote du processus qui a signalé l'écart :
        // ni le rôle de l'étape — le responsable qualité, qui vient de la prononcer — ni la
        // structure du dossier, qui a changé six étapes plus tôt quand il a été confié au processus
        // destinataire.
        etape.setDestinataireCourriel("PILOTE@STRUCTURE_EMETTRICE");

        UUID instanceId = UUID.randomUUID();
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .resourceId("42").resourceType("NON_CONFORMITE")
                .structureId("structure-destinataire")
                .structureEmettriceId("structure-emettrice")
                .build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(destinatairesEtapeService.destinatairesDuRole("PILOTE", "structure-emettrice"))
                .thenReturn(List.of(destinataire("pilote@exemple.fr", "Awa Traoré")));

        notificateur.notifier(etape, new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(),
                "circuit", "42", "8", "9", "responsable-qualite", "dossier clos"));

        verify(destinatairesEtapeService).destinatairesDuRole("PILOTE", "structure-emettrice");
        verify(destinatairesEtapeService, never())
                .destinatairesDuRole(eq("VERIFICATEUR"), any());
    }

    @Test
    @DisplayName("Un dossier sans structure d'origine retombe sur celle où il se trouve")
    void structureEmettriceAbsente_repliSurLaCourante() {
        // Les dossiers ouverts avant la colonne n'en portent pas. Ne prévenir personne serait pire
        // que de prévenir la structure courante, qui est la même tant que rien n'a été orienté.
        etape.setDestinataireCourriel("PILOTE@STRUCTURE_EMETTRICE");

        UUID instanceId = UUID.randomUUID();
        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .resourceId("42").resourceType("NON_CONFORMITE")
                .structureId("structure-courante")
                .build();
        when(validationInstanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(destinatairesEtapeService.destinatairesDuRole("PILOTE", "structure-courante"))
                .thenReturn(List.of(destinataire("pilote@exemple.fr", "Awa Traoré")));

        notificateur.notifier(etape, new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), instanceId.toString(),
                "circuit", "42", "8", "9", "responsable-qualite", "dossier clos"));

        verify(destinatairesEtapeService).destinatairesDuRole("PILOTE", "structure-courante");
    }

    @Test
    @DisplayName("Le gabarit peut nommer l'auteur de la décision")
    void variables_portentLAuteur() {
        // Les messages livrés disent « soumise par X », « mise en œuvre par Y » : sans ce nom, ils
        // décrivaient une action dont l'auteur restait anonyme, et le destinataire devait ouvrir le
        // dossier rien que pour savoir à qui répondre.
        destinataires(destinataire("claire@exemple.fr", "Claire Martin"));
        when(destinatairesEtapeService.destinataire("agent-qualite"))
                .thenReturn(List.of(destinataire("moussa@exemple.fr", "Moussa Kanté")));

        notificateur.notifier(etape, evenement());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> aVariables = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                anyString(), anyString(), anyString(), aVariables.capture());

        assertThat(aVariables.getValue()).containsEntry("auteur", "Moussa Kanté");
    }

    @Test
    @DisplayName("Un auteur introuvable laisse le nom vide, jamais un identifiant technique")
    void auteurIntrouvable_nomVide() {
        destinataires(destinataire("claire@exemple.fr", "Claire Martin"));
        when(destinatairesEtapeService.destinataire(anyString())).thenReturn(List.of());

        notificateur.notifier(etape, evenement());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> aVariables = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                anyString(), anyString(), anyString(), aVariables.capture());

        // « soumise par 8f3c-… » ne dit rien à personne ; la phrase se lit encore sans le nom.
        assertThat(aVariables.getValue()).containsEntry("auteur", "");
    }

    @Test
    @DisplayName("Une étape sans modèle d'e-mail n'entraîne aucune résolution ni envoi")
    void etapeSansModele_rien() {
        etape.setEmailTemplateCode(null);

        notificateur.notifier(etape, evenement());

        verify(destinatairesEtapeService, never()).destinatairesDuRole(anyString(), any());
        verify(notificationService, never()).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Un modèle introuvable n'envoie rien plutôt qu'un message vide")
    void modeleIntrouvable_aucunEnvoi() {
        etape.setEmailTemplateCode("MODELE_ABSENT");
        when(emailTemplateRepository.findByCode("MODELE_ABSENT")).thenReturn(Optional.empty());

        notificateur.notifier(etape, evenement());

        verify(notificationService, never()).enregistrerCourriel(
                nullable(String.class), nullable(String.class),
                anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("L'envoi est bien porté par un bean distinct, condition pour qu'il soit asynchrone")
    void envoi_surUnBeanDistinct() throws Exception {
        // @Async ne s'applique qu'aux appels traversant le proxy Spring : porté par une méthode de
        // WorkflowEventListener, qui l'appelait directement, l'envoi serait resté synchrone.
        assertThat(NotificateurEtapeParEmail.class.getMethod("notifier", WorkflowStep.class,
                TransitionFranchieEvent.class)
                .isAnnotationPresent(Async.class))
                .isTrue();
        assertThat(WorkflowEventListener.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("notifierParEmail"));
    }
}
