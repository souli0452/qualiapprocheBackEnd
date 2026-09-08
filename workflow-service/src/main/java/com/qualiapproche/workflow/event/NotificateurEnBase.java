package com.qualiapproche.workflow.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.dto.DepotNotificationDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.workflow.config.LienVersLeDossier;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.service.NotificationsUtilisateurService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dépose en base ce que le courriel annonce : le dossier a bougé, quelqu'un doit y regarder.
 *
 * <p>Second canal du même événement, avec les <b>mêmes destinataires</b> — {@link DestinatairesDeLEtape}
 * répond pour les deux. Les avoir résolus séparément aurait fini par prévenir des personnes
 * différentes par courriel et à l'écran, ce qu'aucun utilisateur n'aurait su expliquer.</p>
 *
 * <p>Le message n'a pas de gabarit : il reprend le nom que le circuit donne à l'étape atteinte.
 * Une étape renommée dans l'éditeur change donc le texte, et une étape ajoutée s'annonce sans qu'un
 * gabarit ait été écrit pour elle — c'est la différence avec le courriel, qui n'envoie rien sans
 * {@code emailTemplateCode}.</p>
 *
 * <p>Asynchrone et après commit, comme l'envoi du courriel : la décision de l'utilisateur ne doit
 * pas attendre l'écriture de ses notifications, ni échouer si elle échoue.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificateurEnBase {

    /** Repère stable de la nature de la ligne, sur lequel l'écran branche son icône. */
    public static final String CODE = "DOSSIER_A_DECIDER";

    private final DestinatairesDeLEtape destinatairesDeLEtape;
    private final NotificationsUtilisateurService notifications;
    private final LienVersLeDossier lienVersLeDossier;

    @Async
    public void notifier(WorkflowStep step, TransitionFranchieEvent event) {
        List<DestinataireDto> destinataires;
        try {
            destinataires = destinatairesDeLEtape.resoudre(step, event);
        } catch (Exception e) {
            log.error("Destinataires de l'étape « {} » non résolus, aucune notification déposée : {}",
                    step.getNomEtape(), e.getMessage());
            return;
        }

        String reference = reference(event);
        String lien = lienVersLeDossier.pour(event.getResourceType(), event.getResourceId());

        for (DestinataireDto destinataire : destinataires) {
            try {
                notifications.deposer(DepotNotificationDto.builder()
                        .destinataireId(destinataire.getUserId())
                        // L'étape entre dans la clé : un dossier qui repasse par la même étape après
                        // un renvoi doit se réannoncer, et non se confondre avec son premier passage.
                        .cleUnicite(CODE + ":" + event.getResourceId() + ":" + step.getCode())
                        .code(CODE)
                        .source(source(event.getResourceType()))
                        .titre(step.getNomEtape())
                        .message(message(step, reference))
                        .resourceId(event.getResourceId())
                        .resourceType(event.getResourceType())
                        .lien(lien)
                        .gravite(GraviteNotification.INFO)
                        // Le dossier revient : la ligne redevient non lue, sans quoi un dossier
                        // renvoyé deux fois passerait inaperçu la seconde.
                        .reveiller(true)
                        .build());
            } catch (Exception e) {
                // L'échec de l'un ne prive pas les autres, et la cloche reste juste de toute façon :
                // c'est elle qui dit ce qu'il y a à faire, celle-ci ne fait que le raconter.
                log.error("Notification non déposée pour {} à l'étape « {} » : {}",
                        destinataire.getUserId(), step.getNomEtape(), e.getMessage());
            }
        }
    }

    /**
     * La phrase affichée.
     *
     * <p>Sans référence lisible — un dossier ouvert avant que la colonne n'existe — la formule tient
     * quand même : mieux vaut « Un dossier attend votre décision » qu'une phrase trouée.</p>
     */
    private String message(WorkflowStep step, String reference) {
        return reference == null || reference.isBlank()
                ? "Un dossier attend votre décision à l'étape « " + step.getNomEtape() + " »."
                : "Le dossier " + reference + " attend votre décision à l'étape « "
                        + step.getNomEtape() + " ».";
    }

    private String reference(TransitionFranchieEvent event) {
        try {
            return destinatairesDeLEtape.surLInstance(event, WorkflowValidationInstance::getReferenceLisible);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Le domaine dont la ligne provient, déduit de la famille de ressources.
     *
     * <p>Volontairement tolérant : une famille inconnue vaut mieux servie sous son propre nom que
     * rejetée. L'écran groupe ce qu'il connaît et affiche le reste à la suite.</p>
     */
    private String source(String resourceType) {
        return resourceType == null ? "WORKFLOW" : resourceType;
    }
}
