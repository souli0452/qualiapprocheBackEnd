package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reprise des rôles responsables préfixés.
 *
 * <p>{@code WorkflowDataInitializer} ne recrée les circuits que sur une base vierge : sans cette
 * normalisation, une installation en service garderait des rôles {@code ROLE_*} que plus aucun
 * rôle applicatif ne porte — donc aucun titulaire habilité, ni destinataire à notifier.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleResponsableNormalizerTest {

    @Mock private WorkflowRepository workflowRepository;

    @InjectMocks private RoleResponsableNormalizer normalizer;

    private Workflow circuit(String... rolesDesEtapes) {
        Workflow aCircuit = Workflow.builder().nom("Validation NC").resourceType("NON_CONFORMITE").build();
        aCircuit.setId(UUID.randomUUID());

        List<WorkflowStep> aEtapes = new ArrayList<>();
        long id = 1;
        for (String aRole : rolesDesEtapes) {
            WorkflowStep aEtape = new WorkflowStep();
            aEtape.setId(id++);
            aEtape.setNomEtape("Étape " + aRole);
            aEtape.setResponsableRole(aRole);
            aEtape.setWorkflow(aCircuit);
            aEtapes.add(aEtape);
        }
        aCircuit.setSteps(aEtapes);
        return aCircuit;
    }

    @Test
    @DisplayName("Le préfixe ROLE_ est retiré des étapes, le reste du nom est conservé")
    void prefixeRetire_resteConserve() {
        Workflow aCircuit = circuit("ROLE_AGENT", "ROLE_PILOTE", "ROLE_RESPONSABLE_QUALITE", "ROLE_AGENT_IMPUTE");
        when(workflowRepository.findAll()).thenReturn(List.of(aCircuit));

        normalizer.run();

        assertThat(aCircuit.getSteps()).extracting(WorkflowStep::getResponsableRole)
                .containsExactly("AGENT", "PILOTE", "RESPONSABLE_QUALITE", "AGENT_IMPUTE");
    }

    @Test
    @DisplayName("L'habilitation propre d'une transition est normalisée elle aussi")
    void habilitationDeTransition_normalisee() {
        Workflow aCircuit = circuit("ROLE_PILOTE");
        WorkflowStep aEtape = aCircuit.getSteps().getFirst();
        aEtape.setTransitions(new ArrayList<>(List.of(
                WorkflowTransition.builder().id(10L).fromStep(aEtape)
                        .decision(StepDecision.APPROUVE).requiredRole("ROLE_RESPONSABLE_QUALITE").build())));
        when(workflowRepository.findAll()).thenReturn(List.of(aCircuit));

        normalizer.run();

        // Elle prime sur le rôle de l'étape : la laisser préfixée rendrait le correctif partiel.
        assertThat(aEtape.getTransitions().getFirst().getRequiredRole()).isEqualTo("RESPONSABLE_QUALITE");
    }

    @Test
    @DisplayName("Un circuit déjà normalisé n'est pas réenregistré")
    void dejaNormalise_aucuneEcriture() {
        when(workflowRepository.findAll()).thenReturn(List.of(circuit("AGENT", "PILOTE")));

        normalizer.run();

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("L'opération est idempotente : un second passage ne change plus rien")
    void idempotent() {
        Workflow aCircuit = circuit("ROLE_AGENT");
        when(workflowRepository.findAll()).thenReturn(List.of(aCircuit));

        normalizer.run();
        normalizer.run();

        assertThat(aCircuit.getSteps().getFirst().getResponsableRole()).isEqualTo("AGENT");
        verify(workflowRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    @DisplayName("Le circuit modifié est horodaté, pour que le catalogue du moteur se recharge")
    void circuitModifie_horodate() {
        Workflow aCircuit = circuit("ROLE_AGENT");
        when(workflowRepository.findAll()).thenReturn(List.of(aCircuit));

        normalizer.run();

        // La signature du catalogue repose sur cette date ; la seule mise à jour des étapes
        // n'aurait pas touché la ligne du circuit, et les autres instances auraient continué
        // à servir les anciens rôles.
        assertThat(aCircuit.getUpdateAt()).isNotNull();
        verify(workflowRepository).save(aCircuit);
    }

    @Test
    @DisplayName("Un rôle absent ou réduit au seul préfixe est laissé tel quel")
    void roleAbsentOuVide_inchange() {
        Workflow aCircuit = circuit("ROLE_", "AGENT");
        aCircuit.getSteps().getFirst().setResponsableRole(null);
        when(workflowRepository.findAll()).thenReturn(List.of(aCircuit));

        normalizer.run();

        assertThat(aCircuit.getSteps().getFirst().getResponsableRole()).isNull();
        assertThat(aCircuit.getSteps().get(1).getResponsableRole()).isEqualTo("AGENT");
        verify(workflowRepository, never()).save(any());
    }
}
