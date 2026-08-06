package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.impl.PlanActionServiceImpl;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Ce que le circuit décide du plan d'action, et ce que le plan en retient.
 *
 * <p>Un plan décliné doit pouvoir être confié à quelqu'un d'autre. La désignation se fait dans le
 * circuit, mais si elle n'atteint pas le plan lui-même, l'écran continue d'afficher l'ancien
 * responsable : le moteur et la fiche nomment alors deux personnes différentes, et l'on ne sait plus
 * laquelle répond de l'action corrective.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanActionSuitLeCircuitTest {

    @Mock private PlanActionRepository planActionRepository;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;

    @InjectMocks private PlanActionServiceImpl service;

    private PlanAction plan;
    private final UUID ancienResponsable = UUID.randomUUID();

    @BeforeEach
    void planConfie() {
        plan = new PlanAction();
        plan.setId(UUID.randomUUID());
        plan.setNonConformeId(UUID.randomUUID());
        plan.setResponsableId(ancienResponsable);
        when(planActionRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
    }

    @Test
    @DisplayName("La ré-attribution inscrit le nouveau responsable sur le plan")
    void reattribution_changeLeResponsable() {
        UUID nouveau = UUID.randomUUID();

        service.updateWorkflowState(plan.getId(), "À Traiter", "NON_TRAITER",
                Map.of("responsableId", nouveau.toString()));

        assertThat(plan.getResponsableId()).isEqualTo(nouveau);
    }

    @Test
    @DisplayName("Une transition sans désignation laisse le responsable en place")
    void transitionOrdinaire_neTouchePasAuResponsable() {
        // La plupart des franchissements ne nomment personne : ils ne doivent pas effacer ce que
        // l'attribution avait établi.
        service.updateWorkflowState(plan.getId(), "Traité", "TRAITER", Map.of());

        assertThat(plan.getResponsableId()).isEqualTo(ancienResponsable);
    }

    @Test
    @DisplayName("Le compte rendu de la réalisation est consigné sur l'action")
    void realisation_consigneLeCompteRendu() {
        // Il était saisi sur l'écran de traitement puis enregistré par un appel distinct de la
        // décision. Les deux partaient ensemble, dans un ordre que rien ne garantissait : la
        // décision passée la première faisait quitter à l'action l'étape de réalisation, seule où le
        // compte rendu est encore accepté, et ce que le responsable avait écrit était refusé sans
        // que rien ne le signale.
        service.updateWorkflowState(plan.getId(), "Réalisation à vérifier", "EN_VERIFICATION",
                Map.of("causeIdentifiees", "Procédure non diffusée",
                        "solutionRetenues", "Diffusion et séance d'information"));

        assertThat(plan.getCauseIdentifiees()).isEqualTo("Procédure non diffusée");
        assertThat(plan.getSolutionRetenues()).isEqualTo("Diffusion et séance d'information");
    }

    @Test
    @DisplayName("Un franchissement sans compte rendu n'efface pas celui qui est en place")
    void transitionOrdinaire_neVidePasLeCompteRendu() {
        plan.setCauseIdentifiees("Procédure non diffusée");
        plan.setSolutionRetenues("Diffusion et séance d'information");

        // Le pilote confirme la réalisation : il ne rapporte rien de son côté, et ce que le
        // responsable a écrit est précisément ce sur quoi il se prononce.
        service.updateWorkflowState(plan.getId(), "Efficacité à mesurer", "EFFICACITE_A_MESURER",
                Map.of("causeIdentifiees", "  "));

        assertThat(plan.getCauseIdentifiees()).isEqualTo("Procédure non diffusée");
        assertThat(plan.getSolutionRetenues()).isEqualTo("Diffusion et séance d'information");
    }

    @Test
    @DisplayName("Une désignation illisible ne fait pas échouer le franchissement")
    void designationIllisible_neBloquePas() {
        // La valeur vient du réseau. Perdre l'étape franchie parce qu'un identifiant est mal formé
        // coûterait plus cher que de conserver l'ancien responsable et de le signaler.
        service.updateWorkflowState(plan.getId(), "À Traiter", "NON_TRAITER",
                Map.of("responsableId", "pas-un-identifiant"));

        assertThat(plan.getResponsableId()).isEqualTo(ancienResponsable);
        assertThat(plan.getWorkflowStatus()).isEqualTo("À Traiter");
    }
}
