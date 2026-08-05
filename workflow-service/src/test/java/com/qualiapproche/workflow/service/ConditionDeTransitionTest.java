package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.dto.WorkflowTransitionDto;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La condition d'une transition, entre l'éditeur de circuits et la base.
 *
 * <p>La colonne existait et le moteur la respectait, mais aucun DTO ne la portait : une condition
 * posée depuis l'éditeur n'atteignait jamais le serveur, et une condition posée en base
 * n'apparaissait jamais à l'écran. Rien ne le signalait — la transition s'enregistrait, sa
 * condition disparaissait, et l'on ne s'en apercevait qu'en voyant un dossier franchir une étape
 * qu'on croyait verrouillée.</p>
 */
class ConditionDeTransitionTest {

    private final WorkflowMapper mapper = new WorkflowMapper();

    private WorkflowTransition transitionAvecCondition(String condition) {
        return WorkflowTransition.builder()
                .decision(StepDecision.APPROUVE)
                .toStep(WorkflowStep.builder().code("CLOTURE").nomEtape("Clôture").stepOrder(9).build())
                .conditionRequise(condition)
                .build();
    }

    @Test
    @DisplayName("La condition posée en base parvient à l'écran")
    void lecture() {
        WorkflowTransitionDto dto = mapper.toDto(transitionAvecCondition("PLANS_ACTION_SOLDES"));

        assertThat(dto.getConditionRequise()).isEqualTo("PLANS_ACTION_SOLDES");
    }

    @Test
    @DisplayName("La condition saisie à l'écran parvient à la base")
    void ecriture() {
        WorkflowTransitionDto dto = WorkflowTransitionDto.builder()
                .decision("APPROUVE")
                .conditionRequise("PLANS_ACTION_SOLDES")
                .build();

        assertThat(mapper.toEntity(dto).getConditionRequise()).isEqualTo("PLANS_ACTION_SOLDES");
    }

    @Test
    @DisplayName("Une condition saisie autrement reste reconnue")
    void normalisation() {
        WorkflowTransitionDto dto = WorkflowTransitionDto.builder()
                .decision("APPROUVE")
                .conditionRequise("  plans_action_soldes ")
                .build();

        // Le module métier déclare son fait en majuscules. Une casse ou un espace de plus laissait
        // la transition fermée sans que rien ne le dise, et l'on cherchait la faute du côté du
        // module qui déclarait pourtant bien son fait.
        assertThat(mapper.toEntity(dto).getConditionRequise()).isEqualTo("PLANS_ACTION_SOLDES");
    }

    @Test
    @DisplayName("Un champ laissé vide ne pose aucune condition")
    void aucuneCondition() {
        // La chaîne vide doit valoir « aucune condition » et non « un fait sans nom », qui
        // fermerait la transition pour toujours : aucun module ne déclarera jamais ce fait-là.
        assertThat(mapper.toEntity(WorkflowTransitionDto.builder().decision("APPROUVE")
                .conditionRequise("   ").build()).getConditionRequise()).isNull();
        assertThat(mapper.toEntity(WorkflowTransitionDto.builder().decision("APPROUVE")
                .build()).getConditionRequise()).isNull();
    }

    @Test
    @DisplayName("Une transition sans condition se relit sans condition")
    void allerRetourSansCondition() {
        WorkflowTransitionDto dto = mapper.toDto(transitionAvecCondition(null));

        assertThat(dto.getConditionRequise()).isNull();
    }
}
