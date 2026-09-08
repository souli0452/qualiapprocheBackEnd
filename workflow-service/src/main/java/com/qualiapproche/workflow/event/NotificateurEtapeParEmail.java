package com.qualiapproche.workflow.event;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.workflow.model.EmailTemplate;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.EmailTemplateRepository;
import com.qualiapproche.workflow.config.LienVersLeDossier;
import com.qualiapproche.workflow.service.SmtpEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.WorkflowNotificationService;

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
    private final WorkflowNotificationService notificationService;
    private final DestinatairesDeLEtape destinatairesDeLEtape;
    private final com.qualiapproche.workflow.service.DestinatairesEtapeService destinatairesEtapeService;
    /** Adresse du dossier dans le frontal, par type de ressource : le bouton des courriels. */
    private final LienVersLeDossier lienVersLeDossier;
    private final WorkflowValidationInstanceRepository
            validationInstanceRepository;
    private final com.qualiapproche.workflow.service.StructureUtilisateurService structureUtilisateurService;


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

        List<DestinataireDto> destinataires = destinatairesDeLEtape.resoudre(step, event);
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
                // L'objet est rendu ici, à l'enregistrement : la notification stockée porte le
                // texte final, et une reprise le remet tel quel.
                Map<String, String> variables = variables(template, step, event, destinataire);
                var notification = notificationService.enregistrerCourriel(
                        event.getResourceId(), event.getResourceType(),
                        destinataire.getEmail(),
                        SmtpEmailService.sujet(template.getSubject(), variables),
                        template.getBody(), variables);
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
        // La référence lisible du dossier, transmise par le module à l'ouverture du circuit.
        // « numeroNc » est le nom qu'emploient les gabarits livrés ; « reference » le nom neutre,
        // pour les gabarits de documents et de demandes.
        String reference = destinatairesDeLEtape.surLInstance(event, WorkflowValidationInstance::getReferenceLisible);
        variables.put("numeroNc", reference);
        variables.put("reference", reference);

        // Qui vient de décider. Les gabarits livrés le nomment — « soumise par X », « mise en œuvre
        // par Y » : sans lui, chaque message décrivait une action dont l'auteur restait anonyme, et
        // le destinataire devait ouvrir le dossier rien que pour savoir à qui répondre.
        variables.put("auteur", nomDeLAuteur(event));

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
     * Nom de la personne qui vient de décider, lu chez user-service.
     *
     * <p>Une chaîne vide plutôt que l'identifiant technique quand il est introuvable : « soumise par
     * 8f3c-… » ne dit rien à personne, et le gabarit se lit encore sans le nom.</p>
     */
    private String nomDeLAuteur(TransitionFranchieEvent event) {
        List<DestinataireDto> auteur = destinatairesEtapeService.destinataire(event.getAuteurId());
        return auteur.isEmpty() || auteur.get(0).getNomComplet() == null
                ? "" : auteur.get(0).getNomComplet();
    }

    /** Adresse du dossier dans le frontal — vide plutôt qu'un lien mort. */
    private String lienVersLaRessource(TransitionFranchieEvent event) {
        return lienVersLeDossier.pour(event.getResourceType(), event.getResourceId());
    }
}
