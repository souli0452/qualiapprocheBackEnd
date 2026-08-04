package com.qualiapproche.workflow.event;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.service.DestinatairesEtapeService;
import com.qualiapproche.workflow.service.SmtpEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prévient par courriel les responsables de l'étape qu'un dossier vient d'atteindre.
 *
 * <p>Bean à part entière, et non méthode de {@link WorkflowEventListener} : {@code @Async} ne
 * s'applique qu'aux appels passant par le proxy Spring, dont un appel d'une méthode à l'autre au
 * sein d'une même classe s'affranchit. L'envoi serait resté synchrone. La séparation vaut par
 * ailleurs pour elle-même — l'écouteur enregistre et remet des notifications, composer un
 * courriel est un autre métier.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificateurEtapeParEmail {

    private final EmailTemplateRepository emailTemplateRepository;
    private final com.qualiapproche.workflow.service.WorkflowNotificationService notificationService;
    private final DestinatairesEtapeService destinatairesEtapeService;

    /**
     * Motif du lien inséré dans les courriels, avec {@code {resourceType}} et {@code {resourceId}}.
     * À aligner sur les routes réelles du frontal.
     */
    @org.springframework.beans.factory.annotation.Value("${workflow.notifications.motif-lien:}")
    private String motifDeLien;

    /**
     * Compose et envoie le courriel d'étape à chacun de ses responsables.
     *
     * <p>Les destinataires sont résolus auprès de user-service à partir du rôle responsable de
     * l'étape. L'adresse était auparavant fabriquée en accolant le nom du rôle à un domaine
     * ({@code role_VERIFICATEUR@qualiapproche.com}), une boîte qui n'a jamais existé : aucune
     * notification n'atteignait son destinataire, et rien ne le signalait — un envoi vers une
     * adresse syntaxiquement valide n'échoue pas.</p>
     *
     * <p>Asynchrone : l'envoi se déroulait sur le fil de la requête HTTP, après commit, la
     * réponse à l'utilisateur attendant donc le serveur SMTP. Le multiplier par le nombre de
     * responsables aurait rendu ce délai franchement visible.</p>
     */
    @Async
    public void notifier(WorkflowStep step, TransitionFranchieEvent event) {
        String templateCode = step.getEmailTemplateCode();
        if (templateCode == null || templateCode.isBlank()) {
            return;
        }

        EmailTemplate template = emailTemplateRepository.findByCode(templateCode).orElse(null);
        if (template == null) {
            log.warn("Modèle d'e-mail introuvable pour le code '{}'.", templateCode);
            return;
        }

        List<DestinataireDto> destinataires =
                destinatairesEtapeService.destinatairesDuRole(step.getResponsableRole());
        if (destinataires.isEmpty()) {
            log.warn("Étape « {} » atteinte : aucun utilisateur joignable ne porte le rôle {}. "
                            + "Personne n'est prévenu.",
                    step.getNomEtape(), step.getResponsableRole());
            return;
        }

        for (DestinataireDto destinataire : destinataires) {
            // Un envoi par destinataire plutôt qu'un envoi groupé : le corps du message reprend
            // le nom de la personne, et l'échec de l'un ne prive pas les autres.
            try {
                // Enregistré puis remis, plutôt qu'envoyé directement : un serveur SMTP
                // indisponible ou un mot de passe expiré ne perd plus la notification, elle est
                // rejouée par l'ordonnanceur jusqu'à aboutir ou être explicitement abandonnée.
                var notification = notificationService.enregistrerCourriel(
                        event.getResourceId(), event.getResourceType(),
                        destinataire.getEmail(), template.getSubject(), template.getBody(),
                        variables(template, step, event, destinataire));
                notificationService.remettre(notification.getId());
            } catch (Exception e) {
                // Seul l'enregistrement peut encore échouer ici : la remise, elle, est rattrapée
                // par le registre. Le courriel serait alors perdu, d'où la trace.
                log.error("Notification par e-mail '{}' non enregistrée pour {} à l'étape '{}' : {}",
                        templateCode, destinataire.getEmail(), step.getNomEtape(), e.getMessage());
            }
        }
    }

    /**
     * Valeurs exposées au gabarit.
     *
     * <p>Les noms suivent ceux qu'emploient les gabarits livrés — {@code fullName}, {@code link},
     * {@code observation} : les précédents ({@code user}, {@code entityId}, {@code etatApres})
     * n'y figuraient nulle part et n'auraient rien rempli. Les anciens noms sont conservés en plus
     * des nouveaux, pour les gabarits qu'un administrateur aurait écrits en s'y fiant.</p>
     */
    private Map<String, String> variables(EmailTemplate template, WorkflowStep step,
                                          TransitionFranchieEvent event, DestinataireDto destinataire) {
        Map<String, String> variables = new HashMap<>();
        // Noms attendus par les gabarits livrés.
        variables.put("fullName", destinataire.getNomComplet());
        variables.put("link", lienVersLaRessource(event));
        variables.put("observation", event.getCommentaire());
        variables.put("subject", template.getSubject());

        // Contexte du franchissement, pour les gabarits propres au workflow.
        variables.put("etape", step.getNomEtape());
        variables.put("resourceId", event.getResourceId());
        variables.put("resourceType", event.getResourceType());

        // Conservés par compatibilité avec d'éventuels gabarits existants.
        variables.put("user", destinataire.getNomComplet());
        variables.put("entityId", event.getEntityId());
        variables.put("etatAvant", event.getEtatAvant());
        variables.put("etatApres", step.getNomEtape());
        variables.put("auteurId", event.getAuteurId());
        variables.put("commentaire", event.getCommentaire());
        return variables;
    }

    /**
     * Lien vers le dossier dans l'application.
     *
     * <p>Le motif est configurable parce que la route appartient au frontal, que ce service ne
     * connaît pas : le figer ici produirait des liens morts au premier changement de navigation.
     * Vide si aucun motif n'est configuré — le gabarit rend alors un lien sans cible plutôt qu'un
     * lien erroné.</p>
     */
    private String lienVersLaRessource(TransitionFranchieEvent event) {
        if (motifDeLien == null || motifDeLien.isBlank() || event.getResourceId() == null) {
            return "";
        }
        return motifDeLien
                .replace("{resourceType}", event.getResourceType() != null
                        ? event.getResourceType().toLowerCase() : "")
                .replace("{resourceId}", event.getResourceId());
    }
}
