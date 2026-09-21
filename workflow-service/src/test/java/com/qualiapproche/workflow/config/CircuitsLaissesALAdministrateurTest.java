package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Un circuit qui existe en base appartient à l'administrateur qui l'a réglé.
 *
 * <p>Les deux reprises qui reposent des étapes, des routes et des champs sur un circuit existant
 * tournaient à chaque démarrage. Elles ne peuvent pas distinguer « cette étape n'est pas encore
 * arrivée jusqu'ici » de « cette étape a été retirée volontairement » — les deux se ressemblent en
 * base, seule l'intention les sépare et elle n'y est écrite nulle part. Une étape supprimée depuis
 * l'éditeur reparaissait donc au redéploiement suivant, et le travail de configuration était
 * défait sans que personne ne l'ait demandé.</p>
 *
 * <p>Ce test fige le défaut, qui est le comportement attendu en exploitation, et vérifie que la
 * reprise reste possible le jour où une livraison enrichit réellement les circuits.</p>
 */
class CircuitsLaissesALAdministrateurTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(DepotSimule.class)
            .withUserConfiguration(RattrapageDesCircuitsLivres.class, ChampDocumentRejetInitializer.class);

    @Configuration(proxyBeanMethods = false)
    static class DepotSimule {
        @Bean
        WorkflowRepository workflowRepository() {
            return mock(WorkflowRepository.class);
        }
    }

    @Test
    @DisplayName("Au démarrage ordinaire, aucune reprise ne vient réécrire les circuits en base")
    void parDefaut_aucuneRepriseNEstMontee() {
        contexte.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(RattrapageDesCircuitsLivres.class);
            assertThat(ctx).doesNotHaveBean(ChampDocumentRejetInitializer.class);
        });
    }

    @Test
    @DisplayName("La reprise reste disponible, mais sur demande explicite")
    void surDemande_lesReprisesSontMontees() {
        contexte.withPropertyValues("workflow.circuits-livres.rattrapage=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(RattrapageDesCircuitsLivres.class);
            assertThat(ctx).hasSingleBean(ChampDocumentRejetInitializer.class);
        });
    }

    @Test
    @DisplayName("Une valeur autre que « true » ne réveille pas la reprise")
    void valeurInattendue_neReveillePasLaReprise() {
        // Une clé laissée à vide par un déploiement — WORKFLOW_CIRCUITS_RATTRAPAGE non posée —
        // ne doit pas se comporter comme une activation.
        contexte.withPropertyValues("workflow.circuits-livres.rattrapage=").run(ctx ->
                assertThat(ctx).doesNotHaveBean(RattrapageDesCircuitsLivres.class));
    }
}
