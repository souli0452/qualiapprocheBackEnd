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
                    } else {
                        // Terminal state synthesis
                        String endStateCode = "TERMINATED_" + dbTrans.getDecision().name();
                        destination = coreWf.getEtat(endStateCode);
                        if (destination == null) {
                            destination = new Etat(endStateCode);
                            destination.setLibelle("Terminé (" + dbTrans.getDecision().name() + ")");
                            coreWf.addEtat(destination);
                        }
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

    @Override
    public Object getCatalogueVersion() {
        return null; // Pas de versionning pour l'instant
    }
}
