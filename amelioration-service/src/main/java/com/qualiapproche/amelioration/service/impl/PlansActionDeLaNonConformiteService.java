package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.utils.StatutEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Les plans d'action d'une non-conformité, dans leur rapport au circuit qui la pilote.
 *
 * <p>Deux liens unissent les plans au dossier, et ils étaient tous deux implicites :</p>
 * <ul>
 *   <li>quand la non-conformité atteint la validation qualité, chaque plan est <b>confié à son
 *       responsable</b> et entre dans son propre circuit — jusqu'ici cela se faisait par un bouton
 *       qui écrivait les statuts à la main, sans qu'aucun circuit ne soit ouvert ;</li>
 *   <li>la non-conformité ne se clôt que lorsque <b>tous</b> ses plans sont soldés — règle que rien
 *       n'appliquait, si bien qu'on pouvait clore une non-conformité dont les actions correctives
 *       n'avaient pas été menées. C'est la seule chose qu'un système qualité ne doit pas
 *       permettre.</li>
 * </ul>
 *
 * <p>Le second lien passe par un <b>fait</b> déclaré au moteur : le circuit exige la condition sans
 * savoir ce qu'est un plan d'action, et ce service sait quand elle devient vraie sans savoir ce
 * qu'est une transition.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlansActionDeLaNonConformiteService {

    /**
     * Fait exigé par le circuit pour clore. Le nom est le seul lien entre les deux services :
     * il doit valoir exactement celui que déclare {@code WorkflowDataInitializer}.
     */
    public static final String FAIT_PLANS_ACTION_SOLDES = "PLANS_ACTION_SOLDES";

    /**
     * Fait exigé pour que le pilote puisse valider le traitement.
     *
     * <p>L'agent imputé décrit les actions à mener, le pilote en désigne les responsables : une
     * action sans responsable ne pourrait être confiée à personne à la validation qualité, et le
     * dossier attendrait indéfiniment le solde d'une action que nul n'a jamais commencée.</p>
     */
    public static final String FAIT_PLANS_ACTION_AFFECTES = "PLANS_ACTION_AFFECTES";

    private final PlanActionRepository planActionRepository;
    private final WorkflowClient workflowClient;

    /**
     * Confie les plans d'action à leurs responsables et ouvre leur circuit.
     *
     * <p>Un plan déjà engagé n'est pas repris : le franchissement d'une étape peut être rejoué —
     * renvoi puis nouvelle validation — et rouvrir un circuit déjà ouvert ferait repartir de zéro
     * un plan que son responsable a peut-être déjà traité.</p>
     */
    @Transactional
    public void confierLesPlans(UUID nonConformiteId) {
        List<PlanAction> plans = planActionRepository.findPlanActionsByNonConformeId(nonConformiteId);
        if (plans.isEmpty()) {
            return;
        }

        for (PlanAction plan : plans) {
            if (plan.getWorkflowId() != null) {
                continue;
            }
            if (plan.getResponsableId() == null) {
                // Le pilote aurait dû la désigner à la validation ; la condition posée sur cette
                // transition l'en empêche normalement. Ouvrir tout de même le circuit rendrait
                // l'étape de réalisation indécidable : elle est réservée à un titulaire absent.
                log.warn("Action corrective {} sans responsable : elle n'est pas engagée.", plan.getId());
                continue;
            }
            plan.setStatus(StatutEnum.NON_TRAITER);
            ouvrirLeCircuitDuPlan(plan);
            planActionRepository.save(plan);
        }

        // Les plans viennent d'être confiés : aucun n'est soldé, la clôture doit se refermer.
        actualiserLesFaits(nonConformiteId);
    }

    /**
     * Réévalue et déclare l'état de solde des plans auprès du moteur.
     *
     * <p>À appeler chaque fois qu'un plan change de statut. Le fait est retiré autant qu'il est
     * posé : un plan rouvert doit refermer la porte que sa clôture avait ouverte, sans quoi la
     * condition, une fois vraie, le resterait à jamais.</p>
     */
    @Transactional
    public void actualiserLeFaitDeSolde(UUID nonConformiteId) {
        actualiserLesFaits(nonConformiteId);
    }

    /**
     * Réévalue et déclare les deux faits que le circuit de la non-conformité peut exiger de ses
     * actions correctives : qu'elles aient toutes un responsable, et qu'elles soient toutes soldées.
     *
     * <p>Les deux se calculent sur la même lecture. Une non-conformité sans action ne retient rien :
     * les conditions sont remplies d'office, sans quoi elle ne pourrait jamais avancer.</p>
     */
    @Transactional
    public void actualiserLesFaits(UUID nonConformiteId) {
        List<PlanAction> plans = planActionRepository.findPlanActionsByNonConformeId(nonConformiteId);

        boolean toutesAffectees = plans.stream().allMatch(plan -> plan.getResponsableId() != null);
        boolean toutesSoldees = plans.stream().allMatch(plan -> plan.getStatus() == StatutEnum.TRAITER);

        declarer(nonConformiteId, FAIT_PLANS_ACTION_AFFECTES, toutesAffectees);
        declarer(nonConformiteId, FAIT_PLANS_ACTION_SOLDES, toutesSoldees);
    }

    private void declarer(UUID nonConformiteId, String fait, boolean etabli) {
        try {
            workflowClient.declarerFait(nonConformiteId, fait, etabli);
        } catch (Exception e) {
            // La condition ne sera pas remplie : la transition restera fermée jusqu'au prochain
            // changement. Mieux vaut cela qu'une porte ouverte à tort.
            log.warn("Le fait « {} » de la non-conformité {} n'a pas pu être déclaré : {}",
                    fait, nonConformiteId, e.getMessage());
        }
    }

    private void ouvrirLeCircuitDuPlan(PlanAction plan) {
        try {
            WorkflowSummaryDto circuit = workflowClient.getActiveWorkflowByType("PLAN_ACTION");
            if (circuit == null || circuit.getId() == null) {
                log.warn("Aucun circuit actif pour les plans d'action : le plan {} reste hors circuit.",
                        plan.getId());
                return;
            }
            // Le responsable du plan en devient le titulaire : c'est lui, et lui seul, que
            // l'étape de traitement doit ouvrir.
            workflowClient.initiateWorkflow(plan.getId(), "PLAN_ACTION", circuit.getId(),
                    plan.getResponsableId() == null ? null : plan.getResponsableId().toString(),
                    // Les courriels des plans citent la non-conformité d'origine : c'est elle que
                    // le destinataire connaît, le numéro d'ordre du plan ne parle qu'à la fiche.
                    plan.getNumeroNc());
            plan.setWorkflowId(circuit.getId());
        } catch (Exception e) {
            // Le plan reste confié à son responsable même sans circuit : le suivi par le moteur
            // est un plus, l'affectation est l'essentiel.
            log.error("Le circuit du plan d'action {} n'a pas pu être ouvert : {}", plan.getId(), e.getMessage());
        }
    }
}
