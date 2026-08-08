package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.core.engine.WorkflowEngine;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.workflow.core.port.output.IWorkflowEngine;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.qualiapproche.workflow.core.exception.WorkflowException;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WorkflowAppConfig {

    /**
     * Intervalle minimal entre deux contrôles de la signature du catalogue.
     *
     * <p>Borne le coût de détection d'une modification faite par une autre instance : sans lui,
     * chaque consultation du catalogue interrogeait la base. Il ne retarde pas les modifications
     * faites par cette instance, qui recharge explicitement après commit.</p>
     */
    @Value("${workflow.catalogue.intervalle-controle-ms:5000}")
    private long intervalleControleMs;

    @Bean
    public IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> workflowEnginePort(
            IWorkflowEngine<IWorkflowData, TransitionPersistante, WorkflowPersistant> daoPort) {
        WorkflowEngine<IWorkflowData, TransitionPersistante, WorkflowPersistant> engine =
                new WorkflowEngine<>(daoPort, java.time.Duration.ofMillis(intervalleControleMs));
        try {
            engine.init();
        } catch (WorkflowException e) {
            throw new RuntimeException("Failed to initialize WorkflowEngine", e);
        }
        return engine;
    }
}
