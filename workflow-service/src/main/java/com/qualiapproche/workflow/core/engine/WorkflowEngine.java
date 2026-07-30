package com.qualiapproche.workflow.core.engine;

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
 * <p>Sans etat propre au-dela du catalogue charge par {@link #init()} : une instance est
 * partageable entre appels concurrents des lors que {@code init()} a ete appele avant
 * toute utilisation, et n'est pas rappele en cours de service.</p>
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret des transitions
 * @param <W> type concret du workflow
 */
public class WorkflowEngine<D extends IData, T extends Transition<D>, W extends Workflow<D, T>>
        implements IWorkflowEnginePort<D, T, W> {

    private final IWorkflowEngine<D, T, W> daoPort;
    private final SequencedMap<String, W> workflows = new LinkedHashMap<>();
    private final SequencedMap<String, WorkflowConfig<D, T, W>> workflowConfigs =
            new LinkedHashMap<>();
    private final Object verrouRechargement = new Object();
    private volatile Object catalogueVersion;

    /**
     * Construit le moteur a partir du port DAO fournissant le catalogue des workflows.
     *
     * @param pDaoPort port DAO du moteur, non null
     */
    public WorkflowEngine(final IWorkflowEngine<D, T, W> pDaoPort) {
        this.daoPort = Objects.requireNonNull(pDaoPort, "Le port DAO du moteur ne peut etre null.");
    }

    /**
     * Charge en memoire le catalogue des workflows et leurs configurations.
     *
     * @throws WorkflowException si le chargement du catalogue echoue
     */
    @Override
    public void init() throws WorkflowException {
        this.workflows.clear();
        this.workflowConfigs.clear();

        List<W> aWorkflows = Objects.requireNonNullElse(this.daoPort.getAllWorkflow(), List.of());
        aWorkflows.forEach(pWorkflow -> this.workflows.put(pWorkflow.getCode(), pWorkflow));

        List<WorkflowConfig<D, T, W>> aConfigs = Objects.requireNonNullElse(
                this.daoPort.getAllWorkflowConfigs(this.workflows), List.of());
        aConfigs.forEach(pConfig -> this.workflowConfigs.put(pConfig.getWorkflow().getCode(), pConfig));

        this.catalogueVersion = this.daoPort.getCatalogueVersion();
    }

    /**
     * Recharge le catalogue si la source a changé depuis le dernier chargement.
     *
     * <p>Interrogation légère de la signature de la source à chaque consultation ; le
     * catalogue complet n'est rechargé que si la signature diffère. La comparaison et le
     * rechargement sont sérialisés pour qu'un seul appel concurrent recharge, les autres
     * repartant sur le catalogue à jour. Une signature {@code null} — source incapable de
     * se versionner — laisse le catalogue tel quel.</p>
     *
     * @throws WorkflowException si la lecture de la signature ou le rechargement échoue
     */
    private void rafraichirSiPerime() throws WorkflowException {
        Object aCourante = this.daoPort.getCatalogueVersion();
        if (aCourante == null || aCourante.equals(this.catalogueVersion)) {
            return;
        }
        synchronized (this.verrouRechargement) {
            Object aConfirmee = this.daoPort.getCatalogueVersion();
            if (aConfirmee != null && !aConfirmee.equals(this.catalogueVersion)) {
                this.init();
            }
        }
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
        // Contrairement a l'implementation historique, l'absence de configuration est
        // signalee explicitement plutot que par un NullPointerException.
        WorkflowConfig<D, T, W> aConfig = this.configPour(pData);
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
        this.rafraichirSiPerime();
        WorkflowConfig<D, T, W> aConfig = this.configPour(pContexte.getData());

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
        this.configPour(aData);

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
        this.rafraichirSiPerime();
        WorkflowConfig<D, T, W> aConfig = this.configPour(pData);
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
        return this.workflows.get(pCodeWorkflow);
    }

    /**
     * Retourne le catalogue des workflows charges, indexes par code.
     *
     * @return la vue non modifiable des workflows charges
     */
    @Override
    public SequencedMap<String, W> getWorkflow() {
        return Collections.unmodifiableSequencedMap(this.workflows);
    }

    /**
     * Retourne les configurations de workflow chargees, indexees par classe de donnee.
     *
     * @return la vue non modifiable des configurations chargees
     */
    public SequencedMap<String, WorkflowConfig<D, T, W>> getWorkflowConfig() {
        return Collections.unmodifiableSequencedMap(this.workflowConfigs);
    }

    /**
     * Resout la configuration d'une donnee et verifie qu'elle est exploitable.
     * Centralise les trois controles que l'implementation historique repetait a
     * l'identique dans chaque methode publique.
     *
     * @param pData donnee dont la configuration est resolue
     * @return la configuration de workflow associee a la classe de la donnee
     * @throws ConfigurationWorkflowException si la configuration est absente ou incomplete
     */
    private WorkflowConfig<D, T, W> configPour(final D pData) throws ConfigurationWorkflowException {
        String code = pData.getWorkflowCode();
        if (code == null) {
            throw new ConfigurationWorkflowException("Le code du workflow n'est pas defini sur la donnee.");
        }
        WorkflowConfig<D, T, W> aConfig = this.workflowConfigs.get(code);
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
