package com.qualiapproche.workflow.core.exception;

/**
 * Levee lorsque la donnee objet du workflow, son etat, ou la transition demandee
 * sont indefinis au moment de l'execution.
 */
public class DonneeInvalideException extends WorkflowException {

    private static final long serialVersionUID = 1L;

    /**
     * Construit l'exception avec un message.
     *
     * @param pMessage message decrivant la donnee invalide
     */
    public DonneeInvalideException(final String pMessage) {
        super(pMessage);
    }

}
