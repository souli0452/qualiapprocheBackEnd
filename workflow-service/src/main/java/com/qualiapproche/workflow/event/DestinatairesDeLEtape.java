package com.qualiapproche.workflow.event;

import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.utils.RolesPlateforme;
import com.qualiapproche.workflow.model.DestinataireCourriel;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;
import com.qualiapproche.workflow.service.DestinatairesEtapeService;
import com.qualiapproche.workflow.service.StructureUtilisateurService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Qui doit intervenir à l'étape que le dossier vient d'atteindre.
 *
 * <p>Une seule réponse pour tous les canaux. La règle vivait dans le notificateur par courriel,
 * seul à en avoir eu besoin ; un second canal — la notification déposée en base — l'aurait
 * recopiée, et les deux se seraient mises à désigner des personnes différentes au premier cas
 * particulier ajouté d'un côté seulement. Or les cas particuliers sont précisément ce que cette
 * règle contient.</p>
 *
 * <p>Extraite telle quelle : ni le raisonnement ni l'ordre des cas n'ont changé.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DestinatairesDeLEtape {

    private final DestinatairesEtapeService destinatairesEtapeService;
    private final WorkflowValidationInstanceRepository validationInstanceRepository;
    private final StructureUtilisateurService structureUtilisateurService;

    /**
     * La personne que l'étape désigne comme destinataire, ou {@code null} si elle n'en désigne pas.
     *
     * <p>Distinct du <b>rôle responsable</b>, qui dit qui peut décider : une étape ouverte au rôle
     * {@code AGENT} pour que n'importe quel rédacteur puisse reprendre un brouillon doit tout de
     * même annoncer à <b>son</b> auteur que son document lui revient. Confondre les deux envoyait
     * « votre document vous est retourné » à tous les agents du processus, dont aucun ne l'avait
     * écrit.</p>
     *
     * <p>Les deux mêmes désignations que les habilitations — {@code @CREATEUR}, {@code @TITULAIRE} —
     * pour qu'un auteur de circuit n'ait qu'un vocabulaire à connaître.</p>
     */
    private List<DestinataireDto> personneDesignee(WorkflowStep step, TransitionFranchieEvent event) {
        String designation = step.getDestinataireCourriel() == null
                ? "" : step.getDestinataireCourriel().trim();
        if (RolesPlateforme.HABILITATION_CREATEUR.equalsIgnoreCase(designation)) {
            return destinatairesEtapeService.destinataire(
                    surLInstance(event, WorkflowValidationInstance::getCreateurId));
        }
        if (RolesPlateforme.HABILITATION_TITULAIRE.equalsIgnoreCase(designation)) {
            return destinatairesEtapeService.destinataire(
                    surLInstance(event, WorkflowValidationInstance::getTitulaireId));
        }
        return null;
    }

    /**
     * Qui prévenir : les porteurs du rôle de l'étape — dans la structure où le dossier se trouve —
     * ou la seule personne à qui le dossier a été confié.
     *
     * <p>Une étape réservée au titulaire ne porte pas de rôle — la chercher par rôle ne renverrait
     * jamais personne, et l'agent qui vient de recevoir une imputation n'apprendrait par aucun
     * courriel qu'il a un dossier à traiter.</p>
     */
    public List<DestinataireDto> resoudre(WorkflowStep step, TransitionFranchieEvent event) {
        // Une étape peut désigner un autre destinataire que celui qui doit y agir : la clôture d'une
        // non-conformité s'annonce au pilote du processus qui l'a signalée, lequel n'est ni le rôle
        // de l'étape — le responsable qualité — ni, à ce stade, la structure du dossier, qui a été
        // confié au processus destinataire six étapes plus tôt.
        // Une étape peut aussi désigner une **personne** plutôt qu'un rôle : le document retourné à
        // son rédacteur s'annonce à celui qui l'a déposé, non à tous les agents du processus. Ces
        // désignations ne s'écrivent pas RÔLE@PORTÉE — DestinataireCourriel.lire les rend nulles —
        // et sans ce branchement l'étape retomberait sur son rôle responsable.
        List<DestinataireDto> personne = personneDesignee(step, event);
        if (personne != null) {
            return personne;
        }

        DestinataireCourriel designation = DestinataireCourriel.lire(step.getDestinataireCourriel());
        if (designation != null) {
            return destinatairesEtapeService.destinatairesDuRole(
                    designation.role(), structurePour(designation.portee(), event));
        }

        String habilitation = step.getResponsableRole() == null ? "" : step.getResponsableRole().trim();

        if (RolesPlateforme.HABILITATION_TITULAIRE.equalsIgnoreCase(habilitation)) {
            return destinatairesEtapeService.destinataire(
                    surLInstance(event, WorkflowValidationInstance::getTitulaireId));
        }
        // Une étape rendue à son auteur — un dossier renvoyé au déclarant — ne porte pas de rôle non
        // plus : sans cela, celui qu'on attend n'apprend par aucun courriel que son dossier revient.
        if (RolesPlateforme.HABILITATION_CREATEUR.equalsIgnoreCase(habilitation)) {
            return destinatairesEtapeService.destinataire(
                    surLInstance(event, WorkflowValidationInstance::getCreateurId));
        }
        // La structure du dossier borne l'envoi : le rôle d'une étape est porté dans toutes les
        // structures, et l'interroger seul écrivait à la plateforme entière — le supérieur de
        // chaque structure recevait les soumissions de toutes les autres.
        return destinatairesEtapeService.destinatairesDuRole(step.getResponsableRole(),
                structureDuDossier(event));
    }

    /**
     * Structure désignée par une portée : celle où le dossier se trouve, ou celle d'où il vient.
     *
     * <p>Un dossier antérieur à la colonne n'a pas de structure d'origine enregistrée. Retomber sur
     * la structure courante vaut mieux que de ne prévenir personne : c'est la même tant que le
     * dossier n'a pas été orienté ailleurs, et c'est de toute façon la seule connue.</p>
     */
    private String structurePour(DestinataireCourriel.Portee portee, TransitionFranchieEvent event) {
        if (portee == DestinataireCourriel.Portee.STRUCTURE_EMETTRICE) {
            String emettrice = surLInstance(event, WorkflowValidationInstance::getStructureEmettriceId);
            if (emettrice != null && !emettrice.isBlank()) {
                return emettrice;
            }
        }
        return structureDuDossier(event);
    }

    /**
     * Structure où le dossier se trouve — réparée depuis son créateur si elle manque.
     *
     * <p>Les dossiers ouverts avant la colonne, ou pendant que le jeton ne portait pas de
     * structure, n'en ont pas : leurs courriels repartaient vers toute la plateforme. Celle du
     * créateur, lue chez user-service, tient lieu de structure d'origine ; elle est inscrite sur
     * le dossier pour que l'habilitation en profite aussi, pas seulement le prochain courriel.</p>
     */
    private String structureDuDossier(TransitionFranchieEvent event) {
        WorkflowValidationInstance instance;
        try {
            instance = validationInstanceRepository
                    .findById(java.util.UUID.fromString(event.getEntityId()))
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (instance == null) {
            return null;
        }
        if (instance.getStructureId() != null && !instance.getStructureId().isBlank()) {
            return instance.getStructureId();
        }

        String duCreateur = structureUtilisateurService.structureDe(instance.getCreateurId());
        if (duCreateur == null) {
            return null;
        }
        try {
            instance.setStructureId(duCreateur);
            validationInstanceRepository.save(instance);
        } catch (Exception e) {
            // Un conflit d'écriture ne prive personne du courriel : la réparation attendra le
            // prochain franchissement, la valeur résolue sert dès celui-ci.
            log.debug("Structure du dossier {} non inscrite : {}", instance.getResourceId(), e.getMessage());
        }
        return duCreateur;
    }

    /**
     * Une désignation portée par l'instance du circuit — son titulaire, son créateur, sa référence.
     *
     * <p>Publique parce que les canaux en ont besoin au-delà des destinataires : le courriel y lit
     * la référence lisible du dossier pour l'annoncer dans son objet.</p>
     */
    public String surLInstance(TransitionFranchieEvent event,
                                Function<WorkflowValidationInstance, String> designation) {
        try {
            return validationInstanceRepository.findById(java.util.UUID.fromString(event.getEntityId()))
                    .map(designation)
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
