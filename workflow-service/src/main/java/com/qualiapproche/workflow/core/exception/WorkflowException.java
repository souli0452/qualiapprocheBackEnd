package com.qualiapproche.workflow.core.exception;

/**
 * Exception de base du moteur de workflow.
 *
 * <p>Remplace les {@code throw new Exception("...")} de l'implementation historique :
 * les appelants peuvent desormais distinguer une erreur de configuration d'une
 * violation de regle metier.</p>
 */
public class WorkflowException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Construit l'exception avec un message.
     *
     * @param pMessage message decrivant l'erreur
     */
    public WorkflowException(final String pMessage) {
        super(pMessage);
    }

    /**
     * Construit l'exception avec un message et une cause.
     *
     * @param pMessage message decrivant l'erreur
     * @param pCause   cause initiale de l'erreur
     */
    public WorkflowException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }
}
