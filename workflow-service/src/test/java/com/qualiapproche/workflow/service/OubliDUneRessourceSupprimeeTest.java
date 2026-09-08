package com.qualiapproche.workflow.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.ValidationHistoryRepository;
import com.qualiapproche.workflow.repository.WorkflowFieldValueRepository;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import com.qualiapproche.workflow.repository.WorkflowStepFieldRepository;
import com.qualiapproche.workflow.repository.WorkflowStepRepository;
import com.qualiapproche.workflow.repository.WorkflowTransitionRepository;
import com.qualiapproche.workflow.repository.WorkflowValidationInstanceRepository;

/**
 * Ce que le moteur oublie quand un module supprime un dossier.
 *
 * <p>Rien ne le lui disait. Le dossier disparaissait chez son module, son instance restait « en
 * cours » ici, et continuait d'alimenter tout ce qui se compte sans relire la table du module : le
 * tableau de bord par étape annonçait « Traitement 1 » quand la liste « à traiter » — qui, elle, va
 * relire — n'affichait rien. Le défaut ne se voyait donc que sur l'écart entre deux écrans.</p>
 *
 * <p>L'effacement plutôt que la clôture : {@code TERMINE} veut dire « le circuit est allé à son
 * terme », et les tableaux de bord comptent ces dossiers-là parmi les clôturés. Un dossier
 * supprimé n'a pas été clôturé — le compter ainsi remplacerait une surévaluation par une autre.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OubliDUneRessourceSupprimeeTest {

    @Mock private IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur;
    @Mock private ValidationHistoryRepository historyRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private WorkflowValidationInstanceRepository validationInstanceRepository;
    @Mock private WorkflowStepFieldRepository stepFieldRepository;
    @Mock private WorkflowFieldValueRepository fieldValueRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowStepRepository stepRepository;

    private WorkflowService service() {
        return new WorkflowService(moteur, historyRepository, eventPublisher, workflowRepository,
                validationInstanceRepository, stepFieldRepository, fieldValueRepository,
                transitionRepository, stepRepository,
                org.mockito.Mockito.mock(StructureUtilisateurService.class),
                null);
    }

    private WorkflowValidationInstance instance(UUID ressource) {
        WorkflowValidationInstance instance = new WorkflowValidationInstance();
        instance.setId(UUID.randomUUID());
        instance.setResourceId(ressource.toString());
        return instance;
    }

    @Test
    @DisplayName("L'instance et son historique partent avec le dossier")
    void ressourceSupprimee_instanceEtHistoriqueEffaces() {
        UUID ressource = UUID.randomUUID();
        WorkflowValidationInstance instance = instance(ressource);
        when(validationInstanceRepository.findByResourceId(ressource.toString()))
                .thenReturn(List.of(instance));

        assertThat(service().oublierRessource(ressource)).isEqualTo(1);

        // L'historique d'abord : la clé étrangère qu'il porte sur l'instance refuserait l'inverse.
        verify(historyRepository).deleteByValidationInstance_Id(instance.getId());
        verify(validationInstanceRepository).deleteAll(List.of(instance));
    }

    @Test
    @DisplayName("Un dossier rouvert a plusieurs instances : toutes partent, pas la dernière")
    void plusieursInstances_toutesEffacees() {
        // N'en effacer qu'une remettrait un fantôme dans les comptes, et c'est précisément le
        // défaut qu'on corrige.
        UUID ressource = UUID.randomUUID();
        List<WorkflowValidationInstance> instances =
                List.of(instance(ressource), instance(ressource));
        when(validationInstanceRepository.findByResourceId(ressource.toString()))
                .thenReturn(instances);

        assertThat(service().oublierRessource(ressource)).isEqualTo(2);

        verify(historyRepository).deleteByValidationInstance_Id(instances.get(0).getId());
        verify(historyRepository).deleteByValidationInstance_Id(instances.get(1).getId());
        verify(validationInstanceRepository).deleteAll(instances);
    }

    @Test
    @DisplayName("Une ressource que le moteur ne connaît pas n'est pas une erreur")
    void ressourceInconnue_sansEffet() {
        // Un dossier supprimé avant d'avoir été soumis n'a jamais eu d'instance, et le module qui
        // appelle n'a pas à le savoir pour appeler : sans quoi il lui faudrait tenir lui-même la
        // liste des dossiers engagés — une seconde règle, qui finirait par diverger.
        UUID ressource = UUID.randomUUID();
        when(validationInstanceRepository.findByResourceId(ressource.toString()))
                .thenReturn(List.of());

        assertThat(service().oublierRessource(ressource)).isZero();

        verify(historyRepository, never()).deleteByValidationInstance_Id(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Aucun identifiant : rien n'est cherché, et rien n'est effacé")
    void ressourceNulle_riennEstTouche() {
        assertThat(service().oublierRessource(null)).isZero();

        verify(validationInstanceRepository, never()).findByResourceId(anyString());
    }
}
