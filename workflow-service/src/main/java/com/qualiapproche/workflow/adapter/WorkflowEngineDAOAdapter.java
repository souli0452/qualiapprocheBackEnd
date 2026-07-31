package com.qualiapproche.workflow.adapter;

import com.qualiapproche.workflow.core.exception.WorkflowException;
import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.WorkflowConfig;
import com.qualiapproche.workflow.core.port.output.IWorkflowEngine;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngineDAOAdapter implements IWorkflowEngine<IWorkflowData, TransitionPersistante, WorkflowPersistant> {

    private final WorkflowRepository workflowRepository;
    private final WorkflowConditionAdapter conditionAdapter;
    private final BeanFactory beanFactory;

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowPersistant> getAllWorkflow() throws WorkflowException {
        List<com.qualiapproche.workflow.model.Workflow> dbWorkflows = workflowRepository.findAll();
        List<WorkflowPersistant> coreWorkflows = new ArrayList<>();

        for (com.qualiapproche.workflow.model.Workflow dbWf : dbWorkflows) {
            WorkflowPersistant coreWf = new WorkflowPersistant(dbWf.getId().toString());
            coreWf.setLibelle(dbWf.getNom());
            coreWf.setDescription(dbWf.getDescription());

            // Load states
            for (WorkflowStep dbStep : dbWf.getSteps()) {
                Etat etat = new Etat(dbStep.getId().toString());
                etat.setLibelle(dbStep.getNomEtape());
                coreWf.addEtat(etat);
            }

            // Set Initial State
            if (!dbWf.getSteps().isEmpty()) {
                WorkflowStep initialStep = dbWf.getSteps().get(0); // Assuming sorted
                coreWf.setEtatInitial(coreWf.getEtat(initialStep.getId().toString()));
            }

            // Load Transitions
            for (WorkflowStep dbStep : dbWf.getSteps()) {
                for (WorkflowTransition dbTrans : dbStep.getTransitions()) {
                    Etat origine = coreWf.getEtat(dbTrans.getFromStep().getId().toString());
                    Etat destination = null;

                    if (dbTrans.getToStep() != null) {
                        destination = coreWf.getEtat(dbTrans.getToStep().getId().toString());
                    } else if (dbTrans.isTerminal()) {
                        // Fin de circuit explicitement voulue : l'état de sortie est synthétisé.
                        String endStateCode = "TERMINATED_" + dbTrans.getDecision().name();
                        destination = coreWf.getEtat(endStateCode);
                        if (destination == null) {
                            destination = new Etat(endStateCode);
                            destination.setLibelle("Terminé (" + dbTrans.getDecision().name() + ")");
                            coreWf.addEtat(destination);
                        }
                    } else {
                        // Ni destination, ni fin de circuit déclarée : la transition n'a pas été
                        // configurée. L'ignorer plutôt que d'en faire une sortie de circuit évite
                        // de proposer une action que personne n'a voulue — et qui clôturerait le
                        // dossier si elle était déclenchée.
                        log.warn("Transition {} ({}) de l'étape « {} » ignorée : aucune étape de "
                                        + "destination et fin de circuit non déclarée.",
                                dbTrans.getId(), dbTrans.getDecision(), dbStep.getNomEtape());
                        continue;
                    }

                    TransitionPersistante transition = new TransitionPersistante(dbTrans.getId().toString(), origine, destination);
                    transition.setLibelle(dbTrans.getLabel() != null ? dbTrans.getLabel() : dbTrans.getDecision().name());
                    transition.setPermission(habilitationDe(dbTrans, dbStep));

                    // Résoudre l'action via le bean factory (DefaultTransitionAction par défaut si non trouvé)
                    try {
                        @SuppressWarnings("unchecked")
                        com.qualiapproche.workflow.core.interfaces.ITransitionAction<IWorkflowData, TransitionPersistante> action =
                                beanFactory.getBean("workflowStepAction", com.qualiapproche.workflow.core.interfaces.ITransitionAction.class);
                        transition.setAction(action);
                    } catch (Exception e) {
                        log.warn("L'action workflowStepAction n'a pas pu être résolue, on utilisera celle par défaut.", e);
                    }

                    coreWf.addTransition(transition);
                }
            }

            coreWorkflows.add(coreWf);
        }

        return coreWorkflows;
    }

    @Override
    public List<WorkflowConfig<IWorkflowData, TransitionPersistante, WorkflowPersistant>> getAllWorkflowConfigs(
            Map<String, WorkflowPersistant> pWorkflows) throws WorkflowException {
        
        List<WorkflowConfig<IWorkflowData, TransitionPersistante, WorkflowPersistant>> configs = new ArrayList<>();
        
        for (WorkflowPersistant wf : pWorkflows.values()) {
            WorkflowConfig<IWorkflowData, TransitionPersistante, WorkflowPersistant> config = new WorkflowConfig<>(
                    IWorkflowData.class, wf, conditionAdapter);
            configs.add(config);
        }

        return configs;
    }
    
    /**
     * Habilitation exigée pour franchir une transition.
     *
     * <p>{@code requiredRole} porté par la transition prime, mais il n'est renseigné dans aucun
     * circuit existant : sans repli, {@code WorkflowConditionAdapter} autorisait alors tout le
     * monde, et n'importe quel utilisateur authentifié pouvait imputer, valider ou clôturer un
     * dossier. Le {@code responsableRole} de l'étape d'origine — déjà saisi dans les circuits —
     * sert donc de valeur par défaut : c'est bien le rôle censé agir à cette étape.</p>
     */
    private String habilitationDe(WorkflowTransition pTransition, WorkflowStep pEtapeOrigine) {
        String requiredRole = pTransition.getRequiredRole();
        if (requiredRole != null && !requiredRole.isBlank()) {
            return requiredRole;
        }
        String responsableRole = pEtapeOrigine.getResponsableRole();
        if (responsableRole == null || responsableRole.isBlank()) {
            log.warn("La transition {} de l'étape '{}' n'exige aucune habilitation : elle est ouverte "
                            + "à tout utilisateur authentifié.",
                    pTransition.getId(), pEtapeOrigine.getNomEtape());
            return null;
        }
        return responsableRole;
    }

    /**
     * Signature du catalogue, interrogée par le moteur avant chaque consultation pour détecter une
     * modification survenue ailleurs.
     *
     * <p>Elle renvoyait {@code null}, ce qui neutralisait le rechargement : en multi-instance, seul
     * le pod ayant reçu la création ou la modification d'un circuit voyait la nouvelle version, les
     * autres servant un catalogue périmé jusqu'à leur redémarrage. La signature combine le nombre
     * de circuits et la dernière date de modification connue — toute création, modification (y
     * compris d'une étape ou d'une transition, qui passe par l'enregistrement du circuit parent) ou
     * suppression la fait changer.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public Object getCatalogueVersion() {
        return workflowRepository.signatureCatalogue();
    }
}
