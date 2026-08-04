package com.qualiapproche.workflow.core.engine;

import com.qualiapproche.workflow.core.exception.ConfigurationWorkflowException;
import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.core.model.Workflow;
import com.qualiapproche.workflow.core.model.WorkflowConfig;
import com.qualiapproche.workflow.core.port.output.IWorkflowEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du moteur, centrés sur le rechargement conditionnel du catalogue.
 *
 * <p>Ce sont les invariants les plus coûteux à retrouver en production : le catalogue est lu
 * par toutes les requêtes et rechargé sans les interrompre.</p>
 */
class WorkflowEngineTest {

    private static final String CODE_CIRCUIT = "CIRCUIT_TEST";

    // ------------------------------------------------------------------ doublures

    /** Donnée minimale pilotée par le moteur. */
    static class DonneeTest implements IData {
        private transient Etat etat;
        private final String workflowCode;

        DonneeTest(String workflowCode) {
            this.workflowCode = workflowCode;
        }

        @Override public Etat getEtat() { return this.etat; }
        @Override public void setEtat(Etat pEtat) { this.etat = pEtat; }
        @Override public String getWorkflowCode() { return this.workflowCode; }
    }

    /**
     * Source de catalogue pilotable : la signature et le contenu sont fixés par le test, et
     * chaque chargement est compté pour vérifier qu'il n'a lieu que lorsqu'il est nécessaire.
     */
    static class SourceTest implements IWorkflowEngine<DonneeTest, Transition<DonneeTest>, Workflow<DonneeTest, Transition<DonneeTest>>> {

        private volatile String version = "v1";
        private volatile List<String> codesEtats = List.of("A", "B");
        private final AtomicInteger chargements = new AtomicInteger();
        private volatile Runnable pendantChargement = () -> { };
        /** Compte les interrogations de la signature, pour observer leur amortissement. */
        private volatile Runnable observateurDeVersion = () -> { };

        @Override
        public List<Workflow<DonneeTest, Transition<DonneeTest>>> getAllWorkflow() {
            this.chargements.incrementAndGet();
            this.pendantChargement.run();

            Workflow<DonneeTest, Transition<DonneeTest>> aWorkflow = new Workflow<>(CODE_CIRCUIT);
            List<Etat> aEtats = new ArrayList<>();
            for (String aCode : this.codesEtats) {
                Etat aEtat = new Etat(aCode);
                aEtat.setLibelle("Étape " + aCode);
                aWorkflow.addEtat(aEtat);
                aEtats.add(aEtat);
            }
            aWorkflow.setEtatInitial(aEtats.getFirst());

            // Un arc entre chaque état consécutif, pour que le graphe soit exploitable.
            for (int i = 0; i < aEtats.size() - 1; i++) {
                Transition<DonneeTest> aTransition =
                        new Transition<>("T" + i, aEtats.get(i), aEtats.get(i + 1));
                aTransition.setAction(pContexte ->
                        pContexte.getData().setEtat(pContexte.getTransition().getEtatDestination()));
                aWorkflow.addTransition(aTransition);
            }
            return List.of(aWorkflow);
        }

        @Override
        public List<WorkflowConfig<DonneeTest, Transition<DonneeTest>, Workflow<DonneeTest, Transition<DonneeTest>>>>
                getAllWorkflowConfigs(Map<String, Workflow<DonneeTest, Transition<DonneeTest>>> pWorkflows) {
            return pWorkflows.values().stream()
                    .map(pWorkflow -> new WorkflowConfig<>(DonneeTest.class, pWorkflow,
                            (ExecutionContext<DonneeTest> pContexte, Transition<DonneeTest> pTransition) -> true))
                    .toList();
        }

        @Override
        public Object getCatalogueVersion() {
            this.observateurDeVersion.run();
            return this.version;
        }
    }

    private WorkflowEngine<DonneeTest, Transition<DonneeTest>, Workflow<DonneeTest, Transition<DonneeTest>>> moteur(SourceTest pSource)
            throws WorkflowException {
        var aMoteur = new WorkflowEngine<>(pSource);
        aMoteur.init();
        return aMoteur;
    }

    // ------------------------------------------------------------------ rechargement conditionnel

    @Test
    @DisplayName("Le catalogue n'est pas rechargé tant que la signature de la source est inchangée")
    void catalogueStable_pasDeRechargement() throws WorkflowException {
        SourceTest aSource = new SourceTest();
        var aMoteur = moteur(aSource);
        assertThat(aSource.chargements).hasValue(1);

        aMoteur.getWorkflowByCode(CODE_CIRCUIT);
        aMoteur.getWorkflowByCode(CODE_CIRCUIT);
        aMoteur.getTransitionByCode(new DonneeTest(CODE_CIRCUIT), "T0");

        assertThat(aSource.chargements)
                .as("la signature n'a pas bougé : aucune relecture du graphe complet")
                .hasValue(1);
    }

    @Test
    @DisplayName("Un changement de signature déclenche un rechargement, visible dès l'appel suivant")
    void signatureModifiee_rechargement() throws WorkflowException {
        SourceTest aSource = new SourceTest();
        var aMoteur = moteur(aSource);

        aSource.codesEtats = List.of("A", "B", "C");
        aSource.version = "v2";

        assertThat(aMoteur.getWorkflowByCode(CODE_CIRCUIT).getEtat("C"))
                .as("l'étape ajoutée à la source doit être visible sans redémarrage")
                .isNotNull();
        assertThat(aSource.chargements).hasValue(2);
    }

    @Test
    @DisplayName("Une source incapable de se versionner laisse le catalogue en place")
    void signatureNulle_pasDeRechargement() throws WorkflowException {
        SourceTest aSource = new SourceTest();
        var aMoteur = moteur(aSource);

        aSource.version = null;
        aSource.codesEtats = List.of("A", "B", "C");

        assertThat(aMoteur.getWorkflowByCode(CODE_CIRCUIT).getEtat("C")).isNull();
        assertThat(aSource.chargements).hasValue(1);
    }

    @Test
    @DisplayName("Une modification survenue pendant le chargement est détectée au tour suivant")
    void modificationPendantChargement_detectee() throws WorkflowException {
        SourceTest aSource = new SourceTest();

        // La source change au moment même où le moteur lit son graphe : la signature relevée
        // avant lecture reste « v1 », donc plus ancienne que « v2 », et le décalage se voit.
        aSource.pendantChargement = () -> aSource.version = "v2";
        var aMoteur = moteur(aSource);
        aSource.pendantChargement = () -> { };

        aMoteur.getWorkflowByCode(CODE_CIRCUIT);

        assertThat(aSource.chargements)
                .as("relever la signature après la lecture aurait fait passer pour à jour "
                        + "un catalogue déjà périmé")
                .hasValue(2);
    }

    // ------------------------------------------------------------------ publication atomique

    @Test
    @DisplayName("Un lecteur concurrent ne voit jamais un catalogue partiellement rechargé")
    void rechargementConcurrent_aucunCataloguePartiel() throws Exception {
        SourceTest aSource = new SourceTest();
        var aMoteur = moteur(aSource);

        final int aLecteurs = 8;
        final int aLecturesParFil = 400;
        AtomicBoolean aArret = new AtomicBoolean(false);
        AtomicReference<Throwable> aEchec = new AtomicReference<>();
        CountDownLatch aDepart = new CountDownLatch(1);
        CountDownLatch aLecteursTermines = new CountDownLatch(aLecteurs);

        ExecutorService aPool = Executors.newFixedThreadPool(aLecteurs + 1);
        try {
            // Un fil fait muter la source tant que les lecteurs travaillent, forçant des
            // rechargements successifs pendant les lectures.
            aPool.submit(() -> {
                awaitSilencieux(aDepart);
                for (int i = 2; !aArret.get() && aLecteursTermines.getCount() > 0; i++) {
                    aSource.codesEtats = List.of("A", "B", "C" + i);
                    aSource.version = "v" + i;
                    Thread.onSpinWait();
                }
            });

            for (int f = 0; f < aLecteurs; f++) {
                aPool.submit(() -> {
                    awaitSilencieux(aDepart);
                    try {
                        for (int i = 0; i < aLecturesParFil; i++) {
                            var aWorkflow = aMoteur.getWorkflowByCode(CODE_CIRCUIT);
                            // Le circuit existe à toute version : le rendre null ou amputé de
                            // ses états signalerait une lecture pendant la reconstruction.
                            assertThat(aWorkflow).isNotNull();
                            assertThat(aWorkflow.getEtat("A")).isNotNull();
                            assertThat(aWorkflow.getEtats()).hasSizeGreaterThanOrEqualTo(2);
                        }
                    } catch (Throwable t) {
                        aEchec.compareAndSet(null, t);
                    } finally {
                        aLecteursTermines.countDown();
                    }
                });
            }

            aDepart.countDown();
            assertThat(aLecteursTermines.await(30, TimeUnit.SECONDS))
                    .as("les lecteurs doivent terminer : une HashMap lue pendant sa "
                            + "modification structurelle peut ne jamais rendre la main")
                    .isTrue();
        } finally {
            aArret.set(true);
            aPool.shutdownNow();
        }

        assertThat(aEchec.get()).isNull();
        assertThat(aSource.chargements)
                .as("le scénario doit bien avoir provoqué des rechargements")
                .hasValueGreaterThan(1);
    }

    // ------------------------------------------------------------------ controle amorti

    @Test
    @DisplayName("Avec un intervalle, la signature n'est pas interrogée à chaque consultation")
    void intervalleConfigure_signatureAmortie() throws WorkflowException {
        SourceTest aSource = new SourceTest();
        var aMoteur = new WorkflowEngine<>(aSource, Duration.ofMinutes(10));
        aMoteur.init();

        AtomicInteger aControles = new AtomicInteger();
        aSource.observateurDeVersion = aControles::incrementAndGet;

        for (int i = 0; i < 100; i++) {
            aMoteur.getWorkflowByCode(CODE_CIRCUIT);
        }

        assertThat(aControles)
                .as("la signature était interrogée à chaque consultation : trois requêtes d'agrégat "
                        + "par franchissement, et autant que de dossiers pour une liste")
                .hasValue(0);
    }

    @Test
    @DisplayName("Sans intervalle, la signature reste interrogée systématiquement")
    void sansIntervalle_signatureSystematique() throws WorkflowException {
        SourceTest aSource = new SourceTest();
        var aMoteur = moteur(aSource);

        AtomicInteger aControles = new AtomicInteger();
        aSource.observateurDeVersion = aControles::incrementAndGet;

        aMoteur.getWorkflowByCode(CODE_CIRCUIT);
        aMoteur.getWorkflowByCode(CODE_CIRCUIT);

        assertThat(aControles).hasValue(2);
    }

    @Test
    @DisplayName("Un rechargement explicite reste immédiat malgré l'intervalle")
    void intervalleConfigure_rechargementExpliciteImmediat() throws WorkflowException {
        SourceTest aSource = new SourceTest();
        var aMoteur = new WorkflowEngine<>(aSource, Duration.ofMinutes(10));
        aMoteur.init();

        aSource.codesEtats = List.of("A", "B", "C");
        aSource.version = "v2";

        // L'instance qui modifie le circuit recharge après commit : elle ne doit pas attendre
        // l'expiration de l'intervalle pour voir sa propre modification.
        aMoteur.init();

        assertThat(aMoteur.getWorkflowByCode(CODE_CIRCUIT).getEtat("C")).isNotNull();
    }

    // ------------------------------------------------------------------ garde-fous existants

    @Test
    @DisplayName("Une donnée sans code de circuit est refusée explicitement")
    void codeCircuitAbsent_erreurDeConfiguration() throws WorkflowException {
        var aMoteur = moteur(new SourceTest());

        assertThatThrownBy(() -> aMoteur.initEtatData(new DonneeTest(null)))
                .isInstanceOf(ConfigurationWorkflowException.class)
                .hasMessageContaining("code du workflow");
    }

    @Test
    @DisplayName("Un code de circuit inconnu est refusé explicitement")
    void circuitInconnu_erreurDeConfiguration() throws WorkflowException {
        var aMoteur = moteur(new SourceTest());

        assertThatThrownBy(() -> aMoteur.initEtatData(new DonneeTest("CIRCUIT_ABSENT")))
                .isInstanceOf(ConfigurationWorkflowException.class)
                .hasMessageContaining("CIRCUIT_ABSENT");
    }

    private static void awaitSilencieux(CountDownLatch pLatch) {
        try {
            pLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
