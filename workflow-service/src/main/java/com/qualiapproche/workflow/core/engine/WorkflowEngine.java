package com.qualiapproche.workflow.core.engine;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import com.qualiapproche.workflow.core.exception.ConfigurationWorkflowException;
import com.qualiapproche.workflow.core.exception.DonneeInvalideException;
import com.qualiapproche.workflow.core.exception.EtatOrigineInvalideException;
import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.interfaces.IData;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.core.model.Workflow;
import com.qualiapproche.workflow.core.model.WorkflowConfig;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.core.port.output.IWorkflowEngine;

/**
 * Moteur d'execution des workflows.
 *
 * <p>Sans etat propre au-dela du catalogue charge par {@link #init()}, et utilisable par
 * plusieurs fils simultanement, y compris pendant un rechargement : le catalogue est un
 * instantane immuable ({@link Catalogue}) publie par une unique affectation de champ
 * {@code volatile}. Un lecteur voit donc toujours un catalogue complet — l'ancien ou le
 * nouveau, jamais un etat intermediaire.</p>
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret des transitions
 * @param <W> type concret du workflow
 */
public class WorkflowEngine<D extends IData, T extends Transition<D>, W extends Workflow<D, T>>
        implements IWorkflowEnginePort<D, T, W> {

    /**
     * Instantane immuable du catalogue : les deux index et la signature de la source dont
     * ils proviennent, indissociables.
     *
     * <p>Les regrouper est ce qui rend la publication atomique. Portes par trois champs
     * distincts, ils etaient mis a jour l'un apres l'autre : un lecteur pouvait resoudre un
     * etat dans le nouveau catalogue puis ses transitions dans l'ancien. Les vider en place
     * avant de les repeupler etait plus grave encore — lire une {@code HashMap} pendant sa
     * modification structurelle peut rendre {@code null} a tort, voire ne pas terminer.</p>
     */
    private record Catalogue<D extends IData, T extends Transition<D>, W extends Workflow<D, T>>(
            SequencedMap<String, W> workflows,
            SequencedMap<String, WorkflowConfig<D, T, W>> configs,
            Object version) {

        static <D extends IData, T extends Transition<D>, W extends Workflow<D, T>> Catalogue<D, T, W> vide() {
            return new Catalogue<>(
                    Collections.unmodifiableSequencedMap(new LinkedHashMap<>()),
                    Collections.unmodifiableSequencedMap(new LinkedHashMap<>()),
                    null);
        }
    }

    private final IWorkflowEngine<D, T, W> daoPort;
    private final Object verrouRechargement = new Object();
    private final long intervalleControleNanos;
    private volatile Catalogue<D, T, W> catalogue = Catalogue.vide();
    private volatile long dernierControleNanos;

    /**
     * Construit le moteur avec controle systematique de la signature a chaque consultation.
     *
     * @param pDaoPort port DAO du moteur, non null
     */
    public WorkflowEngine(final IWorkflowEngine<D, T, W> pDaoPort) {
        this(pDaoPort, Duration.ZERO);
    }

    /**
     * Construit le moteur en espacant les controles de signature.
     *
     * <p>La signature etait interrogee a <b>chaque</b> consultation du catalogue, soit trois
     * requetes d'agregat pour un seul franchissement de transition, et autant que de dossiers
     * pour l'affichage d'une liste. L'intervalle borne ce cout : passe le premier controle, les
     * consultations suivantes repartent du catalogue en memoire sans toucher la base.</p>
     *
     * <p>Il ne retarde pas la prise en compte d'une modification faite <b>par cette instance</b>,
     * qui recharge explicitement apres commit. Il ne differe que la decouverte d'une modification
     * venue d'une autre instance, au plus de sa duree.</p>
     *
     * @param pDaoPort port DAO du moteur, non null
     * @param pIntervalleControle duree minimale entre deux controles de signature ; zero pour
     *                            controler systematiquement
     */
    public WorkflowEngine(final IWorkflowEngine<D, T, W> pDaoPort, final Duration pIntervalleControle) {
        this.daoPort = Objects.requireNonNull(pDaoPort, "Le port DAO du moteur ne peut etre null.");
        Objects.requireNonNull(pIntervalleControle, "L'intervalle de controle ne peut etre null.");
        this.intervalleControleNanos = Math.max(0L, pIntervalleControle.toNanos());
        // Anterieur d'un intervalle : une consultation precedant le premier chargement controle.
        this.dernierControleNanos = System.nanoTime() - this.intervalleControleNanos;
    }

    /**
     * Charge en memoire le catalogue des workflows et leurs configurations, puis le publie
     * d'un seul coup.
     *
     * <p>La signature de la source est relevee <b>avant</b> la lecture des donnees, et non
     * apres : une modification survenant pendant le chargement produit alors une signature
     * plus recente que celle memorisee, donc detectee a la consultation suivante. Relevee
     * apres, elle aurait fait passer pour a jour un catalogue deja perime.</p>
     *
     * @throws WorkflowException si le chargement du catalogue echoue
     */
    @Override
    public void init() throws WorkflowException {
        Object aVersion = this.daoPort.getCatalogueVersion();

        SequencedMap<String, W> aWorkflowsCharges = new LinkedHashMap<>();
        List<W> aWorkflows = Objects.requireNonNullElse(this.daoPort.getAllWorkflow(), List.of());
        aWorkflows.forEach(pWorkflow -> aWorkflowsCharges.put(pWorkflow.getCode(), pWorkflow));

        SequencedMap<String, WorkflowConfig<D, T, W>> aConfigsChargees = new LinkedHashMap<>();
        List<WorkflowConfig<D, T, W>> aConfigs = Objects.requireNonNullElse(
                this.daoPort.getAllWorkflowConfigs(aWorkflowsCharges), List.of());
        aConfigs.forEach(pConfig -> aConfigsChargees.put(pConfig.getWorkflow().getCode(), pConfig));

        // Publication : unique ecriture visible par les lecteurs.
        this.catalogue = new Catalogue<>(
                Collections.unmodifiableSequencedMap(aWorkflowsCharges),
                Collections.unmodifiableSequencedMap(aConfigsChargees),
                aVersion);
        // Le catalogue vient d'etre lu : le prochain controle peut attendre un intervalle.
        this.dernierControleNanos = System.nanoTime();
    }

    /**
     * Recharge le catalogue si la source a changé depuis le dernier chargement, et rend
     * l'instantané sur lequel l'appelant doit travailler.
     *
     * <p>Interrogation légère de la signature de la source à chaque consultation ; le
     * catalogue complet n'est rechargé que si la signature diffère. La comparaison et le
     * rechargement sont sérialisés pour qu'un seul appel concurrent recharge, les autres
     * repartant sur le catalogue à jour. Une signature {@code null} — source incapable de
     * se versionner — laisse le catalogue tel quel.</p>
     *
     * <p>L'instantané est <b>rendu</b> plutôt que relu ensuite par l'appelant : entre le
     * rafraîchissement et la lecture, un autre fil peut republier: travailler sur la valeur
     * retournée garantit que toute une opération se déroule sur un seul et même catalogue.</p>
     *
     * @return le catalogue à jour au moment de l'appel
     * @throws WorkflowException si la lecture de la signature ou le rechargement échoue
     */
    private Catalogue<D, T, W> rafraichirSiPerime() throws WorkflowException {
        Catalogue<D, T, W> aCatalogue = this.catalogue;
        if (!this.controleDu()) {
            return aCatalogue;
        }
        Object aCourante = this.daoPort.getCatalogueVersion();
        if (aCourante == null || aCourante.equals(aCatalogue.version())) {
            return aCatalogue;
        }
        synchronized (this.verrouRechargement) {
            Object aConfirmee = this.daoPort.getCatalogueVersion();
            if (aConfirmee != null && !aConfirmee.equals(this.catalogue.version())) {
                this.init();
            }
            return this.catalogue;
        }
    }

    /**
     * Indique si la signature de la source doit etre interrogee maintenant, et prend acte de
     * ce controle le cas echeant.
     *
     * <p>Sans intervalle configure, repond toujours oui. Sinon, un seul appel par intervalle
     * obtient l'autorisation : les autres repartent du catalogue en memoire. La course entre
     * appels concurrents est benigne — au pire deux controles rapproches, jamais aucun.</p>
     *
     * @return vrai si la signature doit etre interrogee
     */
    private boolean controleDu() {
        if (this.intervalleControleNanos == 0L) {
            return true;
        }
        long aMaintenant = System.nanoTime();
        // Soustraction et non comparaison directe : seule celle-ci resiste au debordement du
        // compteur de System.nanoTime().
        if (aMaintenant - this.dernierControleNanos < this.intervalleControleNanos) {
            return false;
        }
        this.dernierControleNanos = aMaintenant;
        return true;
    }

    /**
     * Positionne l'etat initial du workflow sur la donnee fournie.
     *
     * @param pData donnee a initialiser
     * @return la donnee dont l'etat a ete initialise
     * @throws WorkflowException si la donnee est indefinie ou sans configuration exploitable
     */
    @Override
    public D initEtatData(final D pData) throws WorkflowException {
        if (pData == null) {
            throw new DonneeInvalideException("La donnee objet du workflow est indefinie.");
        }
        // Le catalogue est charge a la construction du moteur, avant que les initialiseurs de
        // donnees n'aient pu creer les circuits par defaut : sur une base neuve il est donc vide,
        // et toute ouverture de dossier echouait jusqu'au redemarrage suivant. Meme raisonnement
        // qu'ailleurs pour un circuit cree par une autre instance.
        Catalogue<D, T, W> aCatalogue = this.rafraichirSiPerime();
        // Contrairement a l'implementation historique, l'absence de configuration est
        // signalee explicitement plutot que par un NullPointerException.
        WorkflowConfig<D, T, W> aConfig = this.configPour(aCatalogue, pData);
        pData.setEtat(aConfig.getWorkflow().getEtatInitial());
        return pData;
    }

    /**
     * Retourne les transitions franchissables depuis l'etat courant de la donnee du contexte.
     *
     * @param pContexte contexte d'execution portant la donnee evaluee
     * @return l'ensemble non modifiable des transitions autorisees
     * @throws WorkflowException si le contexte ou la donnee est indefini, ou la configuration invalide
     */
    @Override
    public SequencedSet<T> getTransitionsPossibles(final ExecutionContext<D> pContexte) throws WorkflowException {
        if (pContexte == null || pContexte.getData() == null) {
            throw new DonneeInvalideException("La donnee objet du workflow est indefinie.");
        }
        Catalogue<D, T, W> aCatalogue = this.rafraichirSiPerime();
        WorkflowConfig<D, T, W> aConfig = this.configPour(aCatalogue, pContexte.getData());

        return aConfig.getWorkflow().getTransitionsFromEtat(pContexte.getData().getEtat()).stream()
                .filter(pTransition ->
                        aConfig.getTransitionConditionAdapter().estAutorise(pContexte, pTransition))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        Collections::unmodifiableSequencedSet));
    }

    /**
     * Execute une transition sur la donnee du contexte apres validation de l'etat d'origine.
     *
     * @param pContexte contexte d'execution portant la donnee pilotee
     * @param pTransition transition a franchir
     * @throws WorkflowException si le contexte, la donnee, l'etat ou la transition est invalide
     */
    @Override
    public void executerTransition(final ExecutionContext<D> pContexte, final T pTransition)
            throws WorkflowException {
        if (pContexte == null || pContexte.getData() == null) {
            throw new DonneeInvalideException("La donnee objet du workflow est indefinie.");
        }
        if (pTransition == null) {
            throw new DonneeInvalideException("La transition a executer est indefinie.");
        }

        D aData = pContexte.getData();
        if (aData.getEtat() == null) {
            throw new DonneeInvalideException("L'etat de la donnee est indefini.");
        }
        // Valide la configuration avant tout franchissement.
        this.configPour(this.catalogue, aData);

        if (!aData.getEtat().equals(pTransition.getEtatOrigine())) {
            throw new EtatOrigineInvalideException(aData.getEtat(), pTransition.getEtatOrigine());
        }

        pTransition.franchir(pContexte);
    }

    /**
     * Recherche une transition du workflow de la donnee a partir de son code.
     *
     * @param pData donnee dont le workflow est interroge
     * @param pCodeTransition code de la transition recherchee
     * @return la transition correspondante, ou null si aucune ne correspond
     * @throws WorkflowException si la configuration du workflow est invalide
     */
    @Override
    public T getTransitionByCode(final D pData, final String pCodeTransition) throws WorkflowException {
        if (pData == null || pCodeTransition == null || pCodeTransition.isBlank()) {
            return null;
        }
        Catalogue<D, T, W> aCatalogue = this.rafraichirSiPerime();
        WorkflowConfig<D, T, W> aConfig = this.configPour(aCatalogue, pData);
        return aConfig.getWorkflow().getTransitions().stream()
                .filter(pTransition -> pCodeTransition.equals(pTransition.getCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retourne le workflow correspondant au code fourni.
     *
     * @param pCodeWorkflow code du workflow recherche
     * @return le workflow correspondant, ou null si aucun ne correspond
     * @throws WorkflowException si la recherche du workflow echoue
     */
    @Override
    public W getWorkflowByCode(final String pCodeWorkflow) throws WorkflowException {
        // Un dossier peut porter un circuit apparu apres le dernier chargement du catalogue.
        return this.rafraichirSiPerime().workflows().get(pCodeWorkflow);
    }

    /**
     * Retourne le catalogue des workflows charges, indexes par code.
     *
     * @return la vue non modifiable des workflows charges
     */
    @Override
    public SequencedMap<String, W> getWorkflow() {
        return this.catalogue.workflows();
    }

    /**
     * Retourne les configurations de workflow chargees, indexees par classe de donnee.
     *
     * @return la vue non modifiable des configurations chargees
     */
    public SequencedMap<String, WorkflowConfig<D, T, W>> getWorkflowConfig() {
        return this.catalogue.configs();
    }

    /**
     * Resout la configuration d'une donnee et verifie qu'elle est exploitable.
     * Centralise les trois controles que l'implementation historique repetait a
     * l'identique dans chaque methode publique.
     *
     * @param pCatalogue instantane du catalogue sur lequel resoudre
     * @param pData donnee dont la configuration est resolue
     * @return la configuration de workflow associee a la classe de la donnee
     * @throws ConfigurationWorkflowException si la configuration est absente ou incomplete
     */
    private WorkflowConfig<D, T, W> configPour(final Catalogue<D, T, W> pCatalogue, final D pData)
            throws ConfigurationWorkflowException {
        String code = pData.getWorkflowCode();
        if (code == null) {
            throw new ConfigurationWorkflowException("Le code du workflow n'est pas defini sur la donnee.");
        }
        WorkflowConfig<D, T, W> aConfig = pCatalogue.configs().get(code);
        if (aConfig == null) {
            throw new ConfigurationWorkflowException(
                    "Aucun workflow n'est configure pour le code " + code + ".");
        }
        if (aConfig.getWorkflow() == null) {
            throw new ConfigurationWorkflowException(
                    "Workflow non configure pour la classe " + pData.getClass().getName() + ".");
        }
        if (aConfig.getTransitionConditionAdapter() == null) {
            throw new ConfigurationWorkflowException(
                    "Condition d'execution des transitions non configuree pour le workflow "
                            + aConfig.getWorkflow().getCode() + ".");
        }
        return aConfig;
    }
}
