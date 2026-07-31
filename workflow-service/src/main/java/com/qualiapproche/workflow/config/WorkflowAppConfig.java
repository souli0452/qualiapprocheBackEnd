package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.core.engine.WorkflowEngine;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.core.port.output.IWorkflowEngine;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowAppConfig {

    @Bean
    public IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> workflowEnginePort(
            IWorkflowEngine<IWorkflowData, TransitionPersistante, WorkflowPersistant> daoPort) {
        WorkflowEngine<IWorkflowData, TransitionPersistante, WorkflowPersistant> engine = new WorkflowEngine<>(daoPort);
        try {
            engine.init();
        } catch (com.qualiapproche.workflow.core.exception.WorkflowException e) {
            throw new RuntimeException("Failed to initialize WorkflowEngine", e);
        }
        return engine;
    }
}
