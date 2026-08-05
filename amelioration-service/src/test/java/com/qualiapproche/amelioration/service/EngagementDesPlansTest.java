package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.utils.StatutEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le passage d'une action corrective de la proposition à l'engagement.
 *
 * <p>Une action écrite à l'étape de traitement est une <b>proposition</b> : elle n'a pas de circuit,
 * et c'est voulu — l'ouvrir dès l'écriture engagerait un responsable sur une action que personne n'a
 * encore validée. Son circuit s'ouvre à la validation qualité, et c'est là seulement qu'elle devient
 * un engagement nominatif.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EngagementDesPlansTest {

    @Mock private PlanActionRepository planActionRepository;
    @Mock private WorkflowClient workflowClient;

    @InjectMocks private PlansActionDeLaNonConformiteService service;

    private final UUID dossier = UUID.randomUUID();
    private final UUID circuit = UUID.randomUUID();
    private final UUID responsable = UUID.randomUUID();
    private PlanAction plan;

    @BeforeEach
    void planPropose() {
        plan = new PlanAction();
        plan.setId(UUID.randomUUID());
        plan.setNonConformeId(dossier);
        plan.setResponsableId(responsable);
        plan.setStatus(StatutEnum.INACTIF);

        when(planActionRepository.findPlanActionsByNonConformeId(dossier)).thenReturn(List.of(plan));
        WorkflowSummaryDto circuitActif = new WorkflowSummaryDto();
        circuitActif.setId(circuit);
        when(workflowClient.getActiveWorkflowByType("PLAN_ACTION")).thenReturn(circuitActif);
    }

    @Test
    @DisplayName("L'engagement ouvre le circuit de l'action au nom de son responsable")
    void engagement_ouvreLeCircuit() {
        service.confierLesPlans(dossier);

        // Le responsable devient le titulaire : c'est lui, et lui seul, à qui l'étape de réalisation
        // sera ouverte. Un rôle l'aurait ouverte à tout agent, sur toute action.
        verify(workflowClient).initiateWorkflow(plan.getId(), "PLAN_ACTION", circuit, responsable.toString());
        assertThat(plan.getWorkflowId()).isEqualTo(circuit);
        assertThat(plan.getStatus()).isEqualTo(StatutEnum.NON_TRAITER);
    }

    @Test
    @DisplayName("Une action déjà engagée n'est pas réengagée")
    void dejaEngage_nonRejoue() {
        plan.setWorkflowId(circuit);

        service.confierLesPlans(dossier);

        // Le franchissement d'une étape peut être rejoué — renvoi puis nouvelle validation — et
        // rouvrir un circuit ferait repartir de zéro une action déjà menée.
        verify(workflowClient, never()).initiateWorkflow(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("L'engagement referme la clôture du dossier")
    void engagement_refermeLaCloture() {
        service.confierLesPlans(dossier);

        // Les actions viennent d'être confiées : aucune n'est soldée, et la non-conformité ne doit
        // pas pouvoir être close.
        verify(workflowClient).declarerFait(dossier,
                PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_SOLDES, false);
    }

    @Test
    @DisplayName("Un circuit indisponible n'empêche pas de confier l'action")
    void circuitIndisponible_actionConfieeQuandMeme() {
        when(workflowClient.getActiveWorkflowByType("PLAN_ACTION"))
                .thenThrow(new RuntimeException("workflow-service injoignable"));

        service.confierLesPlans(dossier);

        // Le suivi par le moteur est un plus, l'affectation est l'essentiel : l'action reste
        // confiée, et le journal dit qu'elle est hors circuit.
        assertThat(plan.getStatus()).isEqualTo(StatutEnum.NON_TRAITER);
        assertThat(plan.getWorkflowId()).isNull();
    }

    @Test
    @DisplayName("Une action sans responsable ferme la validation du pilote")
    void actionSansResponsable_validationFermee() {
        plan.setResponsableId(null);

        service.actualiserLesFaits(dossier);

        // L'agent peut écrire l'action sans connaître son responsable ; le pilote le désigne à la
        // validation. Valider sans lui laisserait une action que personne ne pourrait mener.
        verify(workflowClient).declarerFait(eq(dossier),
                eq(PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_AFFECTES), eq(false));
    }

    @Test
    @DisplayName("Toutes les actions affectées ouvrent la validation du pilote")
    void toutesAffectees_validationOuverte() {
        service.actualiserLesFaits(dossier);

        verify(workflowClient).declarerFait(eq(dossier),
                eq(PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_AFFECTES), eq(true));
    }

    @Test
    @DisplayName("Une action sans responsable n'est pas engagée")
    void actionSansResponsable_nonEngagee() {
        plan.setResponsableId(null);

        service.confierLesPlans(dossier);

        // Ouvrir son circuit rendrait l'étape de réalisation indécidable : elle est réservée à un
        // titulaire qui n'existe pas.
        verify(workflowClient, never()).initiateWorkflow(any(), anyString(), any(), any());
        assertThat(plan.getWorkflowId()).isNull();
    }

    @Test
    @DisplayName("Une action soldée laisse la clôture s'ouvrir")
    void toutesSoldees_clotureOuverte() {
        plan.setStatus(StatutEnum.TRAITER);

        service.actualiserLeFaitDeSolde(dossier);

        verify(workflowClient).declarerFait(eq(dossier),
                eq(PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_SOLDES), eq(true));
    }

    @Test
    @DisplayName("Une action seulement déclarée réalisée ne compte pas pour soldée")
    void realiseeMaisNonMesuree_clotureFermee() {
        plan.setStatus(StatutEnum.EN_VERIFICATION);

        service.actualiserLeFaitDeSolde(dossier);

        // C'est tout l'objet des étapes de constat et de mesure : une action que son responsable
        // déclare faite n'a encore été vérifiée par personne.
        verify(workflowClient).declarerFait(eq(dossier),
                eq(PlansActionDeLaNonConformiteService.FAIT_PLANS_ACTION_SOLDES), eq(false));
    }
}
