package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.FieldType;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le point de saisie du justificatif de rejet, sur le circuit livré comme sur ceux déjà en base.
 *
 * <p>Sans lui, la référence du fichier déposé n'a aucun chemin jusqu'à la non-conformité : le
 * moteur ne transporte que des valeurs de champs déclarés.</p>
 */
class ChampDocumentRejetTest {

    private final Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();

    private WorkflowRepository workflowRepository;
    private ChampDocumentRejetInitializer rattrapage;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        rattrapage = new ChampDocumentRejetInitializer(workflowRepository);
    }

    @Test
    @DisplayName("Toute étape d'où part un rejet propose de déposer un justificatif")
    void etapesDeRejet_portentLeChamp() {
        List<WorkflowStep> etapesDeRejet = circuit.getSteps().stream()
                .filter(this::permetLeRejet)
                .toList();

        assertThat(etapesDeRejet).isNotEmpty();
        assertThat(etapesDeRejet).allSatisfy(step ->
                assertThat(champDocRejet(step))
                        .withFailMessage("L'étape « %s » permet le rejet mais n'offre aucun dépôt de "
                                + "justificatif : la référence du fichier n'atteindrait jamais la NC.",
                                step.getNomEtape())
                        .isNotNull());
    }

    @Test
    @DisplayName("Le champ est un fichier, et il est facultatif")
    void champ_estUnFichierFacultatif() {
        WorkflowStepField champ = champDocRejet(circuit.getSteps().stream()
                .filter(this::permetLeRejet).findFirst().orElseThrow());

        assertThat(champ.getType()).isEqualTo(FieldType.FILE);
        // La même étape sert à approuver : rendre le justificatif obligatoire y bloquerait
        // l'approbation.
        assertThat(champ.isRequired()).isFalse();
    }

    @Test
    @DisplayName("Une étape sans rejet possible ne demande pas de justificatif")
    void etapesSansRejet_neDemandentRien() {
        assertThat(circuit.getSteps().stream().filter(step -> !permetLeRejet(step)))
                .allSatisfy(step -> assertThat(champDocRejet(step)).isNull());
    }

    @Test
    @DisplayName("Un circuit déjà en base reçoit le champ sur ses seules étapes de rejet")
    void circuitExistant_rattrape() {
        Workflow existant = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        existant.getSteps().forEach(step -> step.getFields()
                .removeIf(champ -> WorkflowDataInitializer.CHAMP_DOCUMENT_REJET.equals(champ.getFieldName())));
        when(workflowRepository.findByResourceType("NON_CONFORMITE")).thenReturn(List.of(existant));

        rattrapage.run();

        verify(workflowRepository).save(existant);
        assertThat(existant.getSteps()).allSatisfy(step ->
                assertThat(champDocRejet(step) != null).isEqualTo(permetLeRejet(step)));
    }

    @Test
    @DisplayName("Le rattrapage repassé une seconde fois ne duplique rien et n'écrit pas")
    void rattrapage_idempotent() {
        when(workflowRepository.findByResourceType("NON_CONFORMITE")).thenReturn(List.of(circuit));

        rattrapage.run();

        // Le circuit livré porte déjà le champ : rien à compléter, donc aucune écriture — et
        // surtout pas un second champ homonyme, que l'écran afficherait deux fois.
        verify(workflowRepository, never()).save(any());
        assertThat(circuit.getSteps()).allSatisfy(step ->
                assertThat(step.getFields().stream()
                        .filter(champ -> WorkflowDataInitializer.CHAMP_DOCUMENT_REJET.equals(champ.getFieldName()))
                        .count())
                        .isLessThanOrEqualTo(1L));
    }

    private boolean permetLeRejet(WorkflowStep step) {
        return step.getTransitions().stream()
                .map(WorkflowTransition::getDecision)
                .anyMatch(StepDecision.REJETE::equals);
    }

    private WorkflowStepField champDocRejet(WorkflowStep step) {
        return step.getFields().stream()
                .filter(champ -> WorkflowDataInitializer.CHAMP_DOCUMENT_REJET.equals(champ.getFieldName()))
                .findFirst()
                .orElse(null);
    }
}
