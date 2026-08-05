package com.qualiapproche.workflow.config;

import com.qualiapproche.common.referentiel.CatalogueEtapesStandard;
import com.qualiapproche.common.referentiel.CatalogueEtapesStandard.EtapeStandard;
import com.qualiapproche.workflow.model.SeveriteAction;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les étapes des circuits livrés par défaut doivent figurer au catalogue d'étapes réutilisables,
 * que support-service sème à partir de {@link CatalogueEtapesStandard}.
 *
 * <p>Les deux listes vivent dans deux services et deux bases distinctes : rien, à l'exécution, ne
 * signalerait qu'une étape ajoutée ici n'a pas été portée là-bas. Le symptôme serait discret —
 * l'éditeur de circuits ne proposerait tout simplement pas cette étape, et l'administrateur la
 * ressaisirait sous un code de son cru, cassant l'agrégation des statistiques que ce code sert
 * précisément à garantir.</p>
 */
class WorkflowDataInitializerCatalogueTest {

    private static final Map<String, EtapeStandard> CATALOGUE = CatalogueEtapesStandard.charger()
            .stream().collect(Collectors.toMap(EtapeStandard::code, Function.identity()));

    private static List<WorkflowStep> etapesLivrees() {
        return Stream.of(
                        WorkflowDataInitializer.circuitDocumentParDefaut(),
                        WorkflowDataInitializer.circuitNonConformiteParDefaut(),
                        WorkflowDataInitializer.circuitPlanActionParDefaut(),
                        WorkflowDataInitializer.circuitDemandeDocumentParDefaut())
                .map(Workflow::getSteps)
                .flatMap(List::stream)
                .toList();
    }

    @Test
    @DisplayName("Toute étape d'un circuit par défaut figure au catalogue partagé")
    void etapesLivrees_figurentAuCatalogue() {
        assertThat(etapesLivrees()).isNotEmpty();

        for (WorkflowStep etape : etapesLivrees()) {
            assertThat(CATALOGUE)
                    .withFailMessage("L'étape « %s » (%s) des circuits par défaut ne figure pas au "
                            + "catalogue : ajoutez-la à workflow-step-catalogue.json, sinon "
                            + "l'éditeur de circuits ne la proposera jamais.",
                            etape.getNomEtape(), etape.getCode())
                    .containsKey(etape.getCode());
        }
    }

    @Test
    @DisplayName("Libellé et rôle responsable concordent entre circuit livré et catalogue")
    void etapesLivrees_concordentAvecLeCatalogue() {
        for (WorkflowStep etape : etapesLivrees()) {
            EtapeStandard entree = CATALOGUE.get(etape.getCode());
            if (entree == null) {
                continue; // signalé par le test précédent
            }
            assertThat(entree.nomEtape())
                    .withFailMessage("%s : libellé « %s » au catalogue, « %s » dans le circuit livré.",
                            etape.getCode(), entree.nomEtape(), etape.getNomEtape())
                    .isEqualTo(etape.getNomEtape());
            assertThat(entree.responsableRole())
                    .withFailMessage("%s : rôle responsable %s au catalogue, %s dans le circuit "
                            + "livré. Le contrôle d'habilitation et la notification des titulaires "
                            + "reposent sur cette égalité.",
                            etape.getCode(), entree.responsableRole(), etape.getResponsableRole())
                    .isEqualTo(etape.getResponsableRole());
        }
    }

    @Test
    @DisplayName("Le catalogue ne décrit pas d'étape orpheline, et aucun code en double")
    void catalogue_resteAlignéSurLesCircuitsLivres() {
        List<EtapeStandard> catalogue = CatalogueEtapesStandard.charger();

        assertThat(catalogue).extracting(EtapeStandard::code).doesNotHaveDuplicates();
        assertThat(catalogue).allSatisfy(entree -> {
            assertThat(entree.code()).isNotBlank();
            assertThat(entree.nomEtape()).isNotBlank();
            // Toujours renseigné : soit un rôle, soit l'habilitation réservant l'étape au
            // titulaire du dossier. Un vide ne dirait ni l'un ni l'autre — et la colonne du
            // modèle d'étape, côté support-service, le refuse.
            assertThat(entree.responsableRole())
                    .withFailMessage("L'étape « %s » ne dit pas qui en décide.", entree.code())
                    .isNotBlank();
        });

        List<String> codesLivres = etapesLivrees().stream().map(WorkflowStep::getCode).toList();
        assertThat(catalogue).extracting(EtapeStandard::code)
                .withFailMessage("Le catalogue décrit une étape qu'aucun circuit par défaut "
                        + "n'emploie ; codes livrés : %s", codesLivres)
                .allMatch(codesLivres::contains);
    }

    private static List<WorkflowTransition> transitionsLivrees() {
        return etapesLivrees().stream()
                .map(WorkflowStep::getTransitions)
                .flatMap(List::stream)
                .toList();
    }

    @Test
    @DisplayName("Toute action livrée porte son libellé, son icône et sa couleur de bouton")
    void transitionsLivrees_apparenceRenseignee() {
        assertThat(transitionsLivrees()).isNotEmpty();

        // Le repli sur la décision existe pour les circuits antérieurs ; les circuits livrés,
        // eux, n'ont pas de raison de s'en remettre à lui.
        assertThat(transitionsLivrees()).allSatisfy(transition -> {
            assertThat(transition.getLabel()).isNotBlank();
            assertThat(transition.getIcon())
                    .withFailMessage("L'action « %s » n'a pas d'icône.", transition.getLabel())
                    .isNotBlank().startsWith("pi pi-");
            assertThat(transition.getSeverity())
                    .withFailMessage("L'action « %s » n'a pas de couleur de bouton.", transition.getLabel())
                    .isNotNull();
        });
    }

    @Test
    @DisplayName("Un renvoi en correction ne s'affiche pas comme un refus ferme")
    void transitionsLivrees_renvoiDistinctDuRefus() {
        // Les deux décisions valent REJETE : sans couleur explicite, elles se présenteraient
        // à l'identique alors que l'une invite à corriger et l'autre arrête le dossier.
        assertThat(transitionsLivrees())
                .filteredOn(t -> t.getDecision() == StepDecision.REJETE)
                .extracting(WorkflowTransition::getSeverity)
                .contains(SeveriteAction.WARN, SeveriteAction.DANGER);
    }
}
