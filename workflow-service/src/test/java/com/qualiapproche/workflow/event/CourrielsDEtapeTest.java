package com.qualiapproche.workflow.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.workflow.config.EmailTemplateEngineConfig;
import com.qualiapproche.workflow.config.LienVersLeDossier;
import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.model.WorkflowNotification;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.repository.WorkflowNotificationRepository;
import com.qualiapproche.workflow.service.AmeliorationWebhookClient;
import com.qualiapproche.workflow.service.CopieAuResponsableQualite;
import com.qualiapproche.workflow.service.DestinatairesEtapeService;
import com.qualiapproche.workflow.service.PiedDeCourriel;
import com.qualiapproche.workflow.service.ReglagesOrganisation;
import com.qualiapproche.workflow.service.SmtpEmailService;
import com.qualiapproche.workflow.service.SupportWebhookClient;
import com.qualiapproche.workflow.service.WorkflowNotificationService;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import jakarta.mail.internet.MimeMultipart;

/**
 * Le courriel d'étape part réellement — de l'étape franchie jusqu'au serveur SMTP.
 *
 * <p>Les autres tests couvrent chacun un maillon : l'enregistrement au registre, la remise, le rendu
 * du gabarit. Celui-ci les enchaîne avec les vraies classes, ne simulant que le serveur SMTP et la
 * base. C'est le seul moyen de montrer qu'un franchissement d'étape produit bien un message remis, et
 * non trois maillons corrects reliés par un chaînon manquant — la panne d'envoi ne s'est pas vue
 * autrement.</p>
 */
class CourrielsDEtapeTest {

    private static final String BOITE_AUTHENTIFIEE = "noreply@qualisira.com";

    private JavaMailSender mailSender;
    private WorkflowNotificationRepository notificationRepository;
    private DestinatairesEtapeService destinatairesEtapeService;
    private WorkflowValidationInstanceRepository instances;
    private EmailTemplateRepository gabaritsPartages;
    private ReglagesOrganisation reglages;
    private NotificateurEtapeParEmail notificateur;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(i -> new MimeMessage((jakarta.mail.Session) null));

        reglages = mock(ReglagesOrganisation.class);
        when(reglages.valeurs()).thenReturn(Map.of());

        SmtpEmailService emailService = new SmtpEmailService(mailSender,
                new EmailTemplateEngineConfig().moteurGabaritEmail(), new PiedDeCourriel(reglages));
        ReflectionTestUtils.setField(emailService, "fromEmail", BOITE_AUTHENTIFIEE);

        notificationRepository = mock(WorkflowNotificationRepository.class);
        when(notificationRepository.save(any(WorkflowNotification.class))).thenAnswer(i -> {
            WorkflowNotification notification = i.getArgument(0);
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID());
            }
            when(notificationRepository.findById(notification.getId()))
                    .thenReturn(Optional.of(notification));
            return notification;
        });
        when(notificationRepository.revendiquer(any(), any(), anyCollection(), any(), any()))
                .thenReturn(1);

        WorkflowNotificationService notificationService = new WorkflowNotificationService(
                notificationRepository, mock(SupportWebhookClient.class), emailService,
                new CopieAuResponsableQualite(reglages), mock(AmeliorationWebhookClient.class),
                new ObjectMapper());
        // Le service s'appelle lui-même par son proxy pour séparer les transactions ; hors
        // conteneur, il se suffit à lui-même.
        ReflectionTestUtils.setField(notificationService, "self", notificationService);

        gabaritsPartages = mock(EmailTemplateRepository.class);
        EmailTemplateRepository gabarits = gabaritsPartages;
        when(gabarits.findByCode("NOTIF_ETAPE")).thenReturn(Optional.of(EmailTemplate.builder()
                .code("NOTIF_ETAPE")
                .subject("Un dossier vous attend")
                .body("<p>Bonjour <span th:text=\"${user}\">x</span>, le dossier "
                        + "<span th:text=\"${resourceId}\">y</span> est à l'étape "
                        + "<span th:text=\"${etape}\">z</span>.</p>")
                .build()));

        destinatairesEtapeService = mock(DestinatairesEtapeService.class);
        instances = mock(WorkflowValidationInstanceRepository.class);
        // La table de routes telle que la configuration la livre : les liens des courriels sont
        // vérifiés jusqu'à l'adresse complète.
        LienVersLeDossier liens = new LienVersLeDossier();
        liens.setBaseUrl("https://qualisira.horeb.tech");
        liens.setLiens(Map.of("NON_CONFORMITE", "/non-conformite/suivi?ncId={resourceId}",
                "DOCUMENT", "/gestion-documentaire/documents?documentId={resourceId}"));
        notificateur = new NotificateurEtapeParEmail(gabarits, notificationService,
                destinatairesEtapeService, liens, instances,
                org.mockito.Mockito.mock(com.qualiapproche.workflow.service.StructureUtilisateurService.class));
    }

    private WorkflowStep etapeVerification() {
        WorkflowStep etape = new WorkflowStep();
        etape.setId(2L);
        etape.setNomEtape("Vérification");
        etape.setResponsableRole("VERIFICATEUR");
        etape.setEmailTemplateCode("NOTIF_ETAPE");
        return etape;
    }

    private TransitionFranchieEvent etapeFranchieSur(String typeRessource, String ressourceId) {
        TransitionFranchieEvent event = new TransitionFranchieEvent(
                WorkflowValidationInstance.class.getName(), UUID.randomUUID().toString(),
                "circuit", "12", "1", "2", "agent-qualite", "avis favorable");
        event.setResourceType(typeRessource);
        event.setResourceId(ressourceId);
        return event;
    }

    private void responsables(DestinataireDto... destinataires) {
        when(destinatairesEtapeService.destinatairesDuRole(eq("VERIFICATEUR"), any()))
                .thenReturn(List.of(destinataires));
    }

    private DestinataireDto claire() {
        return DestinataireDto.builder().userId(UUID.randomUUID().toString())
                .email("claire@exemple.fr").nomComplet("Claire Martin").build();
    }

    private List<MimeMessage> messagesExpedies() {
        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(aMessage.capture());
        return aMessage.getAllValues();
    }

    private String corps(MimeMessage message) throws Exception {
        return extraireHtml(message.getContent());
    }

    private String extraireHtml(Object contenu) throws Exception {
        if (contenu instanceof String texte) {
            return texte;
        }
        if (contenu instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String trouve = extraireHtml(multipart.getBodyPart(i).getContent());
                if (trouve != null) {
                    return trouve;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ l'envoi aboutit

    @Test
    @DisplayName("Une étape franchie fait partir un courriel au responsable, avec objet et expéditeur")
    void etapeFranchie_courrielExpedie() throws Exception {
        responsables(claire());

        notificateur.notifier(etapeVerification(), etapeFranchieSur("DOCUMENT", "42"));

        MimeMessage message = messagesExpedies().get(0);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
                .contains("claire@exemple.fr");
        // Sans expéditeur, le relais authentifié refuse le message.
        assertThat(message.getFrom()[0].toString()).contains(BOITE_AUTHENTIFIEE);
        assertThat(message.getSubject()).isEqualTo("Un dossier vous attend");
    }

    @Test
    @DisplayName("Une étape peut s'annoncer à l'auteur du dossier plutôt qu'aux porteurs de son rôle")
    void destinataireCourriel_auteurDuDossier() throws Exception {
        // Le document retourné à son rédacteur : l'étape reste ouverte au rôle AGENT — n'importe
        // quel rédacteur du processus peut reprendre le brouillon — mais c'est à celui qui l'a
        // déposé qu'on écrit. Sans la désignation, « votre document vous est retourné » partait à
        // tous les agents de la structure, dont aucun ne l'avait écrit.
        WorkflowStep redaction = new WorkflowStep();
        redaction.setId(1L);
        redaction.setNomEtape("Rédaction");
        redaction.setResponsableRole("AGENT");
        redaction.setEmailTemplateCode("NOTIF_ETAPE");
        redaction.setDestinataireCourriel("@CREATEUR");

        TransitionFranchieEvent event = etapeFranchieSur("DOCUMENT", "42");
        DestinataireDto redacteur = DestinataireDto.builder().userId("auteur-du-document")
                .email("redacteur@exemple.fr").nomComplet("Ibrahim Ouédraogo").build();
        when(instances.findById(UUID.fromString(event.getEntityId())))
                .thenReturn(Optional.of(WorkflowValidationInstance.builder()
                        .createurId("auteur-du-document").build()));
        when(destinatairesEtapeService.destinataire("auteur-du-document"))
                .thenReturn(List.of(redacteur));

        notificateur.notifier(redaction, event);

        List<MimeMessage> messages = messagesExpedies();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getRecipients(Message.RecipientType.TO)[0].toString())
                .contains("redacteur@exemple.fr");
        // Le rôle de l'étape n'a pas été interrogé : la désignation prime, elle ne s'y ajoute pas.
        verify(destinatairesEtapeService, org.mockito.Mockito.never())
                .destinatairesDuRole(eq("AGENT"), any());
    }

    @Test
    @DisplayName("Le corps expédié nomme la personne, le dossier et l'étape")
    void courrielExpedie_corpsRenseigne() throws Exception {
        responsables(claire());

        notificateur.notifier(etapeVerification(), etapeFranchieSur("DOCUMENT", "42"));

        String corps = corps(messagesExpedies().get(0));
        assertThat(corps).contains("Claire Martin");
        assertThat(corps).contains("42");
        assertThat(corps).contains("Vérification");
        // Les messages partaient jadis avec leurs attributs Thymeleaf bruts, sans substitution.
        assertThat(corps).doesNotContain("th:text");
    }

    @Test
    @DisplayName("Chaque responsable reçoit son propre message")
    void plusieursResponsables_unMessageChacun() {
        responsables(claire(), DestinataireDto.builder().email("sam@exemple.fr")
                .nomComplet("Sam Diallo").build());

        notificateur.notifier(etapeVerification(), etapeFranchieSur("DOCUMENT", "42"));

        assertThat(messagesExpedies()).hasSize(2);
    }

    @Test
    @DisplayName("Sur une non-conformité, le responsable qualité est en copie du message expédié")
    void etapeDeNonConformite_responsableQualiteEnCopie() throws Exception {
        responsables(claire());
        when(reglages.valeur("RESPONSABLE_QUALITE_EMAIL")).thenReturn("rq@exemple.fr");

        notificateur.notifier(etapeVerification(), etapeFranchieSur("NON_CONFORMITE", "NC-14"));

        assertThat(messagesExpedies().get(0).getRecipients(Message.RecipientType.CC)[0].toString())
                .contains("rq@exemple.fr");
    }

    @Test
    @DisplayName("Sur un document, aucune copie au responsable qualité")
    void etapeDeDocument_sansCopie() throws Exception {
        responsables(claire());
        when(reglages.valeur("RESPONSABLE_QUALITE_EMAIL")).thenReturn("rq@exemple.fr");

        notificateur.notifier(etapeVerification(), etapeFranchieSur("DOCUMENT", "42"));

        assertThat(messagesExpedies().get(0).getRecipients(Message.RecipientType.CC)).isNull();
    }

    // ------------------------------------------------------------------ l'envoi échoue

    @Test
    @DisplayName("Serveur SMTP indisponible : la notification reste à reprendre, elle n'est pas perdue")
    void serveurIndisponible_notificationReprenable() {
        responsables(claire());
        doThrow(new MailSendException("hôte injoignable"))
                .when(mailSender).send(any(MimeMessage.class));

        notificateur.notifier(etapeVerification(), etapeFranchieSur("DOCUMENT", "42"));

        // C'est tout l'intérêt du registre : l'échec était jadis une ligne de journal, et le
        // responsable n'était jamais prévenu.
        ArgumentCaptor<WorkflowNotification> aSauvegardee =
                ArgumentCaptor.forClass(WorkflowNotification.class);
        verify(notificationRepository, org.mockito.Mockito.atLeastOnce()).save(aSauvegardee.capture());
        WorkflowNotification derniere = aSauvegardee.getValue();
        assertThat(derniere.getStatut())
                .isEqualTo(WorkflowNotification.NotificationStatut.A_REMETTRE);
        assertThat(derniere.getDerniereErreur()).contains("hôte injoignable");
    }

    @Test
    @DisplayName("Aucun responsable joignable : rien n'est envoyé, et ce n'est pas silencieux")
    void aucunResponsable_aucunEnvoi() {
        when(destinatairesEtapeService.destinatairesDuRole(eq("VERIFICATEUR"), any())).thenReturn(List.of());

        notificateur.notifier(etapeVerification(), etapeFranchieSur("DOCUMENT", "42"));

        verify(mailSender, org.mockito.Mockito.never()).send(any(MimeMessage.class));
        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Une étape sans gabarit d'e-mail n'envoie rien")
    void etapeSansGabarit_aucunEnvoi() {
        responsables(claire());
        WorkflowStep sansGabarit = etapeVerification();
        sansGabarit.setEmailTemplateCode(null);

        notificateur.notifier(sansGabarit, etapeFranchieSur("DOCUMENT", "42"));

        verify(mailSender, org.mockito.Mockito.never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Le gabarit introuvable n'envoie rien plutôt qu'un message vide")
    void gabaritIntrouvable_aucunEnvoi() {
        responsables(claire());
        WorkflowStep autreGabarit = etapeVerification();
        autreGabarit.setEmailTemplateCode("GABARIT_ABSENT");

        notificateur.notifier(autreGabarit, etapeFranchieSur("DOCUMENT", "42"));

        verify(mailSender, org.mockito.Mockito.never()).send(any(MimeMessage.class));
    }

    // ------------------------------------------------------------------ référence du dossier

    @Test
    @DisplayName("La référence transmise à l'ouverture arrive dans le corps et l'objet du courriel")
    void reference_dansLeCourriel() throws Exception {
        responsables(claire());
        TransitionFranchieEvent event = etapeFranchieSur("NON_CONFORMITE", "NC-UUID");
        WorkflowValidationInstance instance =
                WorkflowValidationInstance.builder()
                        .referenceLisible("NC-2026-014").build();
        when(instances.findById(UUID.fromString(event.getEntityId())))
                .thenReturn(Optional.of(instance));
        // Gabarit et objet écrits comme un rédacteur le ferait : à l'accolade.
        when(gabaritsPartages.findByCode("NOTIF_ETAPE"))
                .thenReturn(Optional.of(EmailTemplate.builder()
                        .code("NOTIF_ETAPE")
                        .subject("Validation attendue – NC {numeroNc}")
                        .body("<p>Le dossier {numeroNc} attend votre décision.</p>")
                        .build()));

        notificateur.notifier(etapeVerification(), event);

        MimeMessage message = messagesExpedies().get(0);
        // Sans la référence, le destinataire recevait « n°__ » : le message partait, mais sans
        // l'information qui identifie le dossier.
        assertThat(message.getSubject()).isEqualTo("Validation attendue – NC NC-2026-014");
        assertThat(corps(message)).contains("Le dossier NC-2026-014 attend votre décision.");
    }

    @Test
    @DisplayName("Sans référence — dossiers antérieurs — l'objet reste propre et le courriel part")
    void sansReference_courrielPropre() throws Exception {
        responsables(claire());
        when(gabaritsPartages.findByCode("NOTIF_ETAPE"))
                .thenReturn(Optional.of(EmailTemplate.builder()
                        .code("NOTIF_ETAPE")
                        .subject("Validation attendue – {numeroNc}")
                        .body("<p>Une décision vous attend. {numeroNc}</p>")
                        .build()));

        notificateur.notifier(etapeVerification(), etapeFranchieSur("NON_CONFORMITE", "NC-UUID"));

        MimeMessage message = messagesExpedies().get(0);
        assertThat(message.getSubject()).isEqualTo("Validation attendue");
        assertThat(corps(message)).doesNotContain("{numeroNc}");
    }

    @Test
    @DisplayName("Le lien du courriel mène à l'écran du type de dossier")
    void lien_versLEcranDuType() throws Exception {
        responsables(claire());
        when(gabaritsPartages.findByCode("NOTIF_ETAPE"))
                .thenReturn(Optional.of(EmailTemplate.builder()
                        .code("NOTIF_ETAPE").subject("Un dossier vous attend")
                        .body("<p><a th:href=\"${link}\">Consulter</a></p>")
                        .build()));

        notificateur.notifier(etapeVerification(), etapeFranchieSur("NON_CONFORMITE", "nc-42"));

        // Un motif unique ne pouvait être juste que pour un type à la fois : chaque famille a sa
        // route, et c'est elle qui doit figurer dans le message.
        assertThat(corps(messagesExpedies().get(0)))
                .contains("https://qualisira.horeb.tech/non-conformite/suivi?ncId=nc-42");
    }
}
