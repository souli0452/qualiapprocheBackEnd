package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.enumeration.Circuit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un plan d'action se soumet entier.
 *
 * <p>La personne à qui la non-conformité est imputée recherche les causes et <b>propose</b> le
 * plan ; son supérieur ne peut se prononcer que sur un plan complet. Il en manquait la moitié : le
 * formulaire n'offrait ni la cause ni la solution retenue, qui n'étaient recueillies que bien plus
 * tard, quand le responsable de l'action déclarait l'avoir menée. Le pilote validait donc une
 * intention, et découvrait à la mise en œuvre que l'action n'avait ni échéance ni critère auquel
 * confronter le résultat.</p>
 *
 * <p>La règle est portée par un <b>fait</b>, et non par un refus à l'enregistrement : un plan
 * s'écrit progressivement, et refuser de sauvegarder une action à demi rédigée ferait perdre à son
 * auteur ce qu'il vient d'écrire. La complétude se vérifie au moment où le plan quitte ses mains.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanCompletAvantSoumissionTest {

    @Mock private PlanActionRepository planActionRepository;
    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private WorkflowClient workflowClient;

    @InjectMocks private PlansActionDeLaNonConformiteService service;

    private final UUID dossier = UUID.randomUUID();
    private NonConformite nonConformite;

    @BeforeEach
    void dossierOuvert() {
        nonConformite = new NonConformite();
        nonConformite.setId(dossier);
        nonConformite.setCircuit(Circuit.ACTION_CORRECTIVE);
        when(nonConformiteRepository.findById(dossier)).thenReturn(Optional.of(nonConformite));
    }

    /** Une action portant les sept colonnes que le document demande. */
    private PlanAction planComplet() {
        PlanAction plan = new PlanAction();
        plan.setId(UUID.randomUUID());
        plan.setNonConformeId(dossier);
        plan.setNumeroOdre("1");
        plan.setCauseIdentifiees("Contrôle non tracé");
        plan.setSolutionRetenues("Fiche de contrôle instaurée");
        plan.setActionCorrective("Rédiger et diffuser la fiche de contrôle");
        plan.setResponsableId(UUID.randomUUID());
        plan.setDateEcheance(LocalDate.of(2026, 9, 30));
        plan.setCritereEfficacite("Aucun contrôle non tracé sur trois mois");
        return plan;
    }

    private void avecLesPlans(PlanAction... plans) {
        when(planActionRepository.findPlanActionsByNonConformeId(dossier)).thenReturn(List.of(plans));
    }

    @Test
    @DisplayName("Un plan portant toutes ses colonnes ouvre la soumission")
    void planComplet_soumissionOuverte() {
        avecLesPlans(planComplet());

        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS, true);
    }

    @Test
    @DisplayName("Sans critère d'efficacité, le plan ne peut pas être soumis")
    void sansCritere_soumissionFermee() {
        // Le critère est ce à quoi le résultat sera confronté à la fin du parcours de l'action.
        // Absent, il n'y a plus rien à mesurer, et l'action se solderait sur sa seule exécution.
        PlanAction plan = planComplet();
        plan.setCritereEfficacite("   ");
        avecLesPlans(plan);

        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS, false);
    }

    @Test
    @DisplayName("En action corrective, la cause est exigée")
    void actionCorrective_causeExigee() {
        PlanAction plan = planComplet();
        plan.setCauseIdentifiees(null);
        avecLesPlans(plan);

        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS, false);
    }

    @Test
    @DisplayName("En correction, la cause ne l'est pas : la colonne n'existe pas")
    void correction_causeNonExigee() {
        // Une correction remet en conformité ce qui ne l'était pas, sans avoir à remonter à ce qui
        // l'a produit. Exiger la cause y ferait écrire n'importe quoi pour passer l'étape.
        nonConformite.setCircuit(Circuit.CORRECTION);
        PlanAction plan = planComplet();
        plan.setCauseIdentifiees(null);
        avecLesPlans(plan);

        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS, true);
    }

    @Test
    @DisplayName("Un dossier sans aucune action ne peut pas être soumis")
    void aucunPlan_soumissionFermee() {
        // Traiter, c'est proposer un plan : soumettre sans en avoir écrit un ferait franchir l'étape
        // sans que rien n'ait été proposé. Les deux autres faits, eux, restent vrais d'office — ils
        // portent sur des actions qui n'existent pas.
        avecLesPlans();

        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS, false);
        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_SOLDES, true);
    }

    @Test
    @DisplayName("Une seule action incomplète referme la soumission de tout le dossier")
    void uneSeuleIncomplete_soumissionFermee() {
        PlanAction incomplet = planComplet();
        incomplet.setDateEcheance(null);
        avecLesPlans(planComplet(), incomplet);

        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS, false);
    }

    @Test
    @DisplayName("Le responsable n'entre pas dans la complétude : le pilote le désigne ensuite")
    void sansResponsable_planTenuPourComplet() {
        // La personne imputée ne le connaît pas toujours. Sa propre condition le réclame une étape
        // plus loin, à la validation du pilote — l'exiger ici bloquerait la soumission sur une
        // information que son auteur n'a pas.
        PlanAction plan = planComplet();
        plan.setResponsableId(null);
        avecLesPlans(plan);

        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_COMPLETS, true);
        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_AFFECTES, false);
    }
}
