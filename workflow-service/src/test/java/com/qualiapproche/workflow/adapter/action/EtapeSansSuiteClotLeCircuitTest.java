package com.qualiapproche.workflow.adapter.action;

import com.qualiapproche.workflow.core.model.ActionExecutionContext;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.model.ValidationStatus;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un circuit se termine de deux façons, et les deux se valent.
 *
 * <p>Par une <b>action</b> déclarée terminale, qui ne mène nulle part ; ou en atteignant une
 * <b>étape</b> qui n'offre aucune action. La seconde est celle qu'emploient les circuits livrés —
 * l'étape « Clôture » d'une non-conformité, l'étape « Soldée » d'un plan d'action — et c'est aussi
 * la plus lisible pour qui monte un circuit : une étape « Clôturer » dit la fin mieux qu'une case
 * cochée sur un bouton.</p>
 *
 * <p>Seule la première était reconnue. Le dossier atteignait son étape de clôture et l'instance y
 * restait « en cours » indéfiniment : jamais achevée, jamais horodatée, et le circuit ne pouvait
 * plus être relancé sur la ressource — un circuit y étant réputé déjà ouvert.</p>
 */
class EtapeSansSuiteClotLeCircuitTest {

    private static final long ETAPE_AVEC_SUITE = 7L;
    private static final long ETAPE_SANS_SUITE = 9L;

    private WorkflowTransitionRepository transitionRepository;
    private WorkflowStepAction action;

    @BeforeEach
    void setUp() {
        transitionRepository = mock(WorkflowTransitionRepository.class);
        action = new WorkflowStepAction(transitionRepository);
    }

    private WorkflowValidationInstance instanceEnCours() {
        return WorkflowValidationInstance.builder()
                .id(UUID.randomUUID())
                .resourceId(UUID.randomUUID().toString())
                .resourceType("DOCUMENT")
                .status(ValidationStatus.EN_COURS)
                .build();
    }

    private void franchir(WorkflowValidationInstance instance, String codeDestination) throws Exception {
        Transition<WorkflowValidationInstance> transition =
                new Transition<>("42", new Etat("1"), new Etat(codeDestination));
        action.update(new ActionExecutionContext<>(instance, transition));
    }

    @Test
    @DisplayName("Atteindre une étape sans action clôt l'instance et l'horodate")
    void etapeSansAction_clotLInstance() throws Exception {
        when(transitionRepository.existsByFromStepId(ETAPE_SANS_SUITE)).thenReturn(false);
        WorkflowValidationInstance instance = instanceEnCours();

        franchir(instance, String.valueOf(ETAPE_SANS_SUITE));

        assertThat(instance.getStatus()).isEqualTo(ValidationStatus.TERMINE);
        assertThat(instance.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Une étape qui offre encore une action laisse le circuit en cours")
    void etapeAvecAction_laisseLeCircuitEnCours() throws Exception {
        when(transitionRepository.existsByFromStepId(ETAPE_AVEC_SUITE)).thenReturn(true);
        WorkflowValidationInstance instance = instanceEnCours();

        franchir(instance, String.valueOf(ETAPE_AVEC_SUITE));

        assertThat(instance.getStatus()).isEqualTo(ValidationStatus.EN_COURS);
        assertThat(instance.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("L'état de sortie d'une action terminale clôt le circuit, sans interroger les étapes")
    void etatTerminalSynthetique_clotLInstance() throws Exception {
        WorkflowValidationInstance instance = instanceEnCours();

        franchir(instance, "TERMINATED_APPROUVE");

        assertThat(instance.getStatus()).isEqualTo(ValidationStatus.TERMINE);
        assertThat(instance.getCompletedAt()).isNotNull();
    }
}
