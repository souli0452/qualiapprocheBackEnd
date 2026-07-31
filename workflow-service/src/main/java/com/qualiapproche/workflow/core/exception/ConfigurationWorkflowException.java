package com.qualiapproche.workflow.core.exception;

/**
 * Levee lorsque le moteur ne trouve pas de configuration exploitable pour une donnee :
 * classe non enregistree, workflow absent, ou port de condition non fourni.
 */
public class ConfigurationWorkflowException extends WorkflowException {

    private static final long serialVersionUID = 1L;

    /**
     * Construit l'exception avec un message.
     *
     * @param pMessage message decrivant l'erreur de configuration
     */
    public ConfigurationWorkflowException(final String pMessage) {
        super(pMessage);
    }

    /**
     * Construit l'exception avec un message et une cause.
     *
     * @param pMessage message decrivant l'erreur de configuration
     * @param pCause   cause initiale de l'erreur
     */
    public ConfigurationWorkflowException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

}
