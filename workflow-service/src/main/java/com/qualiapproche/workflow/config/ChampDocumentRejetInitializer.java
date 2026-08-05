package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Rattrapage des champs d'étape du circuit de non-conformité sur les installations existantes.
 *
 * <p>{@link WorkflowDataInitializer} ne crée le circuit par défaut que si aucun n'existe : sur une
 * installation déjà en service — c'est-à-dire toutes — les champs qu'il déclare n'auraient jamais
 * vu le jour, et les fonctions qui en dépendent seraient restées sans point de saisie.</p>
 *
 * <p>Deux compléments, chacun posé là où il a un sens :</p>
 * <ul>
 *   <li>le <b>justificatif de rejet</b>, sur toute étape d'où part une décision {@code REJETE} —
 *       c'est la présence de la décision qui commande, pas le nom de l'étape, de sorte qu'un
 *       circuit remanié dans l'éditeur est traité comme le circuit livré ;</li>
 *   <li>la <b>structure destinataire</b>, sur les étapes d'orientation du dossier, reconnues par
 *       leur code. Un circuit dont les étapes ont été renommées ou recomposées n'est pas deviné :
 *       l'administrateur y ajoute les champs depuis l'éditeur, sous les mêmes noms.</li>
 * </ul>
 *
 * <p>Une étape qui porte déjà un champ est laissée telle quelle : un administrateur a pu en changer
 * le libellé ou le rendre obligatoire, et ce rattrapage n'a pas à défaire son choix.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(110) // après WorkflowStepCodeInitializer
public class ChampDocumentRejetInitializer implements CommandLineRunner {

    /**
     * Étape du circuit livré qui oriente le dossier vers une structure.
     *
     * <p>Une seule : l'affectation est la décision du responsable qualité. La répartir sur
     * plusieurs étapes aurait rendu impossible de dire qui a orienté le dossier.</p>
     */
    private static final Set<String> CODES_ETAPES_ORIENTATION = Set.of("VALIDATION_RQ");

    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Workflow> circuits = workflowRepository.findByResourceType("NON_CONFORMITE");
        int ajoutes = 0;

        for (Workflow circuit : circuits) {
            boolean modifie = false;
            for (WorkflowStep step : circuit.getSteps()) {
                if (permetLeRejet(step) && !porteLeChamp(step, WorkflowDataInitializer.CHAMP_DOCUMENT_REJET)) {
                    step.getFields().add(WorkflowDataInitializer.champDocumentRejet(step));
                    modifie = true;
                    ajoutes++;
                }
                if (oriente(step)
                        && !porteLeChamp(step, WorkflowDataInitializer.CHAMP_STRUCTURE_DESTINATAIRE_ID)) {
                    step.getFields().add(WorkflowDataInitializer.champStructureDestinataire(step, true));
                    modifie = true;
                    ajoutes++;
                }
            }
            if (modifie) {
                workflowRepository.save(circuit);
                log.info("Champs d'étape complétés sur le circuit « {} »", circuit.getNom());
            }
        }

        if (ajoutes > 0) {
            log.info("{} étape(s) complétée(s) sur les circuits de non-conformité.", ajoutes);
        }
    }

    private boolean permetLeRejet(WorkflowStep step) {
        return step.getTransitions().stream()
                .map(WorkflowTransition::getDecision)
                .anyMatch(StepDecision.REJETE::equals);
    }

    private boolean oriente(WorkflowStep step) {
        return step.getCode() != null && CODES_ETAPES_ORIENTATION.contains(step.getCode());
    }

    private boolean porteLeChamp(WorkflowStep step, String nomDeChamp) {
        return step.getFields().stream()
                .map(WorkflowStepField::getFieldName)
                .anyMatch(nomDeChamp::equals);
    }
}
