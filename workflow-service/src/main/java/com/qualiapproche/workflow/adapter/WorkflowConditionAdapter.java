package com.qualiapproche.workflow.adapter;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.core.model.ExecutionContext;
import com.qualiapproche.workflow.core.port.output.ITransitionCondition;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import org.springframework.stereotype.Component;

@Component
public class WorkflowConditionAdapter implements ITransitionCondition<IWorkflowData, TransitionPersistante> {

    @Override
    public boolean estAutorise(ExecutionContext<IWorkflowData> pContexte, TransitionPersistante pTransition) {
        String requiredRole = pTransition.getPermission();
        if (requiredRole == null || requiredRole.trim().isEmpty()) {
            return true; // No specific role required, allow transition
        }
        
        // Verify if the current user has the required role
        return SecurityUtils.hasRole(requiredRole);
    }
}
