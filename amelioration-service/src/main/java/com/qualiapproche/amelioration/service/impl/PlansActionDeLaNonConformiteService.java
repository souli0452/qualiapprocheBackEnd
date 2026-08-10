package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.common.enumeration.Circuit;
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

    /**
     * Fait exigé pour soumettre le traitement : chaque action proposée est complète.
     *
     * <p>La personne à qui la non-conformité est imputée recherche les causes et propose le plan ;
     * son supérieur ne peut se prononcer que sur un plan entier. Il en manquait la moitié — cause et
     * solution retenue n'étaient recueillies que bien plus tard, au moment où le responsable de
     * l'action déclarait l'avoir menée — si bien que le pilote validait une intention, et non un
     * plan.</p>
     *
     * <p>Un fait, et non un refus à l'enregistrement : un plan s'écrit progressivement, et refuser
     * de sauvegarder une action à demi rédigée ferait perdre à son auteur ce qu'il vient d'écrire.
     * La complétude se vérifie au moment où le plan quitte ses mains.</p>
     */
    public static final String FAIT_PLANS_ACTION_COMPLETS = "PLANS_ACTION_COMPLETS";

    private final PlanActionRepository planActionRepository;
    private final NonConformiteRepository nonConformiteRepository;
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
        // Un dossier sans aucune action ne peut pas non plus être soumis : le traitement consiste
        // précisément à proposer un plan, et l'étape serait franchie sans que rien n'ait été proposé.
        Circuit circuit = plans.isEmpty() ? null : circuitDu(nonConformiteId);
        boolean toutesCompletes = !plans.isEmpty()
                && plans.stream().allMatch(plan -> estComplet(plan, circuit));

        declarer(nonConformiteId, FAIT_PLANS_ACTION_AFFECTES, toutesAffectees);
        declarer(nonConformiteId, FAIT_PLANS_ACTION_SOLDES, toutesSoldees);
        declarer(nonConformiteId, FAIT_PLANS_ACTION_COMPLETS, toutesCompletes);
    }

    /**
     * Une action porte-t-elle toutes les colonnes que le dossier lui demande ?
     *
     * <p>Numéro, solution retenue, action proposée, échéance et critère d'efficacité valent pour
     * tous. La <b>cause</b> dépend du circuit de traitement retenu par le responsable qualité : en
     * correction, on remet en conformité ce qui ne l'était pas sans avoir à remonter à ce qui l'a
     * produit — la colonne n'existe pas, et l'exiger ferait écrire n'importe quoi pour passer.</p>
     *
     * <p>Le responsable n'en fait pas partie : la personne imputée ne le connaît pas toujours, et
     * c'est le pilote qui le désigne à la validation. Sa propre condition
     * ({@link #FAIT_PLANS_ACTION_AFFECTES}) le réclame une étape plus loin.</p>
     */
    private boolean estComplet(PlanAction plan, Circuit circuit) {
        return renseigne(plan.getNumeroOdre())
                && renseigne(plan.getActionCorrective())
                && renseigne(plan.getSolutionRetenues())
                && renseigne(plan.getCritereEfficacite())
                && plan.getDateEcheance() != null
                && (circuit == Circuit.CORRECTION || renseigne(plan.getCauseIdentifiees()));
    }

    private boolean renseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    /**
     * Circuit de traitement du dossier — action corrective ou correction.
     *
     * <p>Un dossier qui n'en porte pas encore est traité comme une action corrective, la plus
     * exigeante des deux : mieux vaut demander la cause à tort que laisser passer un plan qui aurait
     * dû la porter.</p>
     */
    private Circuit circuitDu(UUID nonConformiteId) {
        return nonConformiteRepository.findById(nonConformiteId)
                .map(NonConformite::getCircuit)
                .orElse(Circuit.ACTION_CORRECTIVE);
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
