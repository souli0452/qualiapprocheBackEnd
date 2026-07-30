package com.qualiapproche.workflow.persistence.model;

import com.qualiapproche.workflow.core.model.Workflow;

/**
 * Type alias pour un workflow persistant, évitant les signatures génériques trop longues.
 */
public class WorkflowPersistant extends Workflow<IWorkflowData, TransitionPersistante> {
    private static final long serialVersionUID = 1L;

    public WorkflowPersistant(String code) {
        super(code);
    }
}
