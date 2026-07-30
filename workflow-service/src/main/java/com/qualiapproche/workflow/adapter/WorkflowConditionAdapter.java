package com.qualiapproche.workflow.adapter;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.port.output.ITransitionCondition;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import org.springframework.stereotype.Component;

/**
 * Habilitation d'une transition : l'utilisateur courant doit porter le rôle exigé.
 *
 * <p>Le rôle exigé est résolu par {@code WorkflowEngineDAOAdapter} (habilitation propre à la
 * transition, à défaut rôle responsable de l'étape d'origine).</p>
 */
@Component
public class WorkflowConditionAdapter implements ITransitionCondition<IWorkflowData, TransitionPersistante> {

    /** Rôle d'administration autorisé à débloquer un dossier quelle que soit l'étape. */
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    @Override
    public boolean estAutorise(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        String requiredRole = pTransition.getPermission();
        if (requiredRole == null || requiredRole.trim().isEmpty()) {
            // Transition non restreinte : ouverte à tout utilisateur authentifié.
            return true;
        }

        return SecurityUtils.hasRole(requiredRole) || SecurityUtils.hasRole(ROLE_SUPER_ADMIN);
    }
}
