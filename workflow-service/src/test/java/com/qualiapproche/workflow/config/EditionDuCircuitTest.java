package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.service.WorkflowMapper;
import com.qualiapproche.workflow.dto.WorkflowStepFieldDto;
import com.qualiapproche.workflow.model.FieldType;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.qualiapproche.workflow.dto.WorkflowTransitionDto;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowTransition;

/**
 * Ce que l'enregistrement d'un circuit depuis l'éditeur conserve du circuit précédent.
 *
 * <p>L'aller-retour passe par un DTO : toute propriété que celui-ci ne porte pas est perdue au
 * premier enregistrement, sans que rien ne le signale — le circuit s'enregistre, la propriété
 * disparaît, et l'on ne s'en aperçoit qu'au dossier suivant, quand une étape n'ouvre plus à
 * personne ou qu'un champ est réclamé au mauvais moment.</p>
 */
class EditionDuCircuitTest {

    private final WorkflowMapper mapper = new WorkflowMapper();

    @Test
    @DisplayName("La désignation du titulaire survit à un enregistrement")
    void champTitulaire_conserve() {
        WorkflowStep etape = WorkflowStep.builder()
                .id(1L).code("IMPUTATION").nomEtape("Imputation").stepOrder(4)
                .responsableRole("PILOTE")
                .champTitulaire("userImputId")
                .build();

        WorkflowStepDto relu = mapper.toDto(etape);

        // Sans lui, l'étape d'imputation cesse de nommer quelqu'un, et les étapes réservées au
        // titulaire deviennent indécidables : plus personne ne peut faire avancer le dossier.
        assertThat(relu.getChampTitulaire()).isEqualTo("userImputId");
    }

    @Test
    @DisplayName("La portée d'un champ survit à un enregistrement")
    void porteeDuChamp_conservee() {
        WorkflowStepField champ = WorkflowStepField.builder()
                .id(7L).fieldName("docRejet").fieldLabel("Justificatif du rejet")
                .type(FieldType.FILE)
                .decision(StepDecision.REJETE)
                .build();

        WorkflowStepFieldDto relu = mapper.toDto(champ);

        // Perdue, le justificatif de rejet redevient un champ de toutes les décisions : il est
        // demandé à l'approbation comme au rejet.
        assertThat(relu.getDecision()).isEqualTo(StepDecision.REJETE.name());
    }

    @Test
    @DisplayName("La condition d'une transition et son explication survivent à un enregistrement")
    void condition_conservee() {
        WorkflowTransition transition =
                WorkflowTransition.builder()
                        .decision(StepDecision.APPROUVE)
                        .conditionRequise("PLANS_ACTION_SOLDES")
                        .conditionLibelle("toutes les actions correctives sont soldées")
                        .build();

        WorkflowTransitionDto relu = mapper.toDto(transition);

        assertThat(relu.getConditionRequise()).isEqualTo("PLANS_ACTION_SOLDES");
        assertThat(relu.getConditionLibelle()).isEqualTo("toutes les actions correctives sont soldées");
    }

    @Test
    @DisplayName("Le circuit livré traverse l'aller-retour sans rien perdre")
    void circuitLivre_allerRetourComplet() {
        // Le circuit de non-conformité porte toutes les propriétés à risque à la fois : désignation
        // du titulaire, portée de champ, source de choix, condition. S'il survit, le contrat tient.
        Workflow circuit =
                WorkflowDataInitializer.circuitNonConformiteParDefaut();

        for (WorkflowStep etape : circuit.getSteps()) {
            WorkflowStepDto relu = mapper.toDto(etape);
            assertThat(relu.getChampTitulaire()).isEqualTo(etape.getChampTitulaire());
            assertThat(relu.getResponsableRole()).isEqualTo(etape.getResponsableRole());

            for (WorkflowStepField champ : etape.getFields()) {
                WorkflowStepFieldDto champRelu = relu.getFields().stream()
                        .filter(f -> f.getFieldName().equals(champ.getFieldName()))
                        .findFirst().orElseThrow();
                assertThat(champRelu.getOptions()).isEqualTo(champ.getOptions());
                assertThat(champRelu.getDecision())
                        .isEqualTo(champ.getDecision() == null ? null : champ.getDecision().name());
                assertThat(champRelu.getActionCode()).isEqualTo(champ.getActionCode());
            }
        }
    }

    @Test
    @DisplayName("Le code d'une action survit à un enregistrement")
    void codeDeLAction_conserve() {
        WorkflowTransition transition =
                WorkflowTransition.builder()
                        .code("DEMANDER_COMPLEMENT")
                        .decision(StepDecision.APPROUVE)
                        .label("Demander un complément")
                        .build();

        WorkflowTransitionDto relu = mapper.toDto(transition);

        // C'est le code qui identifie l'action dans son étape : perdu, deux actions de même nature
        // se confondent, et l'enregistrement suivant en efface une.
        assertThat(relu.getCode()).isEqualTo("DEMANDER_COMPLEMENT");
    }

    @Test
    @DisplayName("Le champ propre à une action survit à un enregistrement")
    void actionDuChamp_conservee() {
        WorkflowStepField champ = WorkflowStepField.builder()
                .id(9L).fieldName("precisionsAttendues").fieldLabel("Précisions attendues")
                .type(FieldType.TEXT)
                .decision(StepDecision.APPROUVE)
                .actionCode("DEMANDER_COMPLEMENT")
                .build();

        WorkflowStepFieldDto relu = mapper.toDto(champ);

        // Perdue, la question posée par « Demander un complément » serait posée à qui valide
        // simplement : les deux actions approuvent.
        assertThat(relu.getActionCode()).isEqualTo("DEMANDER_COMPLEMENT");
    }

    @Test
    @DisplayName("Une action sans code se nomme d'après sa décision")
    void actionSansCode_prendCelleDeSaDecision() {
        WorkflowTransition transition =
                WorkflowTransition.builder()
                        .decision(StepDecision.REJETE)
                        .build();

        // Les circuits antérieurs n'ont pas de code : ils portaient de fait celui de leur décision.
        // Sans ce repli, rien ne les apparierait plus au circuit livré.
        assertThat(transition.codeEffectif()).isEqualTo("REJETE");
    }
}
