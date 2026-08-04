package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.TypeRessource;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le circuit livré pour les demandes de modification et de suppression.
 *
 * <p>Trois de ses propriétés ne se voient pas à la lecture et se paient très cher à l'exécution :
 * s'il n'est pas <b>actif</b>, aucune demande ne peut être déposée — support-service cherche le
 * circuit actif de la famille et refuse tout le reste ; si sa dernière étape n'est pas
 * <b>terminale et déclarée</b>, le moteur ignore la transition et l'aboutissement n'a jamais lieu,
 * si bien qu'aucun document ne serait ni remplacé ni supprimé ; et si sa famille n'est pas
 * reconnue, la configuration est refusée à l'enregistrement.</p>
 */
class CircuitDemandeDocumentTest {

    private final Workflow circuit = WorkflowDataInitializer.circuitDemandeDocumentParDefaut();

    @Test
    @DisplayName("Le circuit est actif : sans cela, aucune demande ne peut être déposée")
    void circuit_estActif() {
        // support-service interroge `getActiveWorkflowByType` et refuse le dépôt s'il ne trouve
        // rien. Un circuit créé inactif rendrait la fonctionnalité entière inutilisable, sans que
        // rien ne le signale avant la première demande.
        assertThat(circuit.isActif()).isTrue();
    }

    @Test
    @DisplayName("La famille du circuit est reconnue par le moteur")
    void circuit_familleReconnue() {
        assertThat(circuit.getResourceType()).isEqualTo("DEMANDE_DOCUMENT");
        assertThat(TypeRessource.normaliser(circuit.getResourceType())).isEqualTo("DEMANDE_DOCUMENT");
        assertThat(TypeRessource.valeursAutorisees()).contains("DEMANDE_DOCUMENT");
    }

    @Test
    @DisplayName("Les deux issues de la décision sont terminales et déclarées comme telles")
    void decision_estTerminale() {
        WorkflowStep decision = etape("DEMANDE_DECISION");

        List<WorkflowTransition> issues = decision.getTransitions();
        assertThat(issues).hasSize(2);
        assertThat(issues)
                .withFailMessage("Une transition sans destination ni marqueur terminal est ignorée "
                        + "par le moteur : l'aboutissement — remplacement ou suppression — n'aurait "
                        + "jamais lieu.")
                .allSatisfy(transition -> {
                    assertThat(transition.isTerminal()).isTrue();
                    assertThat(transition.getToStep()).isNull();
                });
        assertThat(issues).extracting(WorkflowTransition::getDecision)
                .containsExactlyInAnyOrder(StepDecision.APPROUVE, StepDecision.REJETE);
    }

    @Test
    @DisplayName("Le rejet en instruction renvoie au demandeur, il ne clôt pas la demande")
    void rejetEnInstruction_renvoieAuDemandeur() {
        WorkflowTransition rejet = etape("DEMANDE_INSTRUCTION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.REJETE)
                .findFirst().orElseThrow();

        // Une demande refusée en instruction doit pouvoir être reprise et réargumentée : la clore
        // obligerait à tout ressaisir.
        assertThat(rejet.isTerminal()).isFalse();
        assertThat(rejet.getToStep()).isNotNull();
        assertThat(rejet.getToStep().getCode()).isEqualTo("DEMANDE_SOUMISSION");
    }

    @Test
    @DisplayName("L'instruction revient au responsable qualité")
    void instruction_confieeAuResponsableQualite() {
        assertThat(etape("DEMANDE_INSTRUCTION").getResponsableRole()).isEqualTo("RESPONSABLE_QUALITE");
        assertThat(etape("DEMANDE_DECISION").getResponsableRole()).isEqualTo("RESPONSABLE_QUALITE");
    }

    @Test
    @DisplayName("Les étapes se suivent sans trou dans l'ordre")
    void etapes_ordonnees() {
        assertThat(circuit.getSteps()).extracting(WorkflowStep::getStepOrder)
                .containsExactly(1, 2, 3);
    }

    private WorkflowStep etape(String code) {
        return circuit.getSteps().stream()
                .filter(step -> code.equals(step.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Étape absente du circuit : " + code));
    }
}
