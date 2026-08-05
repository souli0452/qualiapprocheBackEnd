package com.qualiapproche.workflow.persistence.model;

import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.model.SeveriteAction;
import com.qualiapproche.workflow.model.StepDecision;
import lombok.Getter;
import lombok.Setter;

/**
 * Transition construite depuis la base, enrichie de son habilitation et autres méta-données.
 */
@Getter
@Setter
public class TransitionPersistante extends Transition<IWorkflowData> {
    private static final long serialVersionUID = 1L;
    private String permission; // Equivalent to requiredRole

    /**
     * Fait exigé du dossier pour que la transition soit franchissable, ou {@code null}.
     * Voir {@code WorkflowTransition#conditionRequise}.
     */
    private String conditionRequise;
    private Integer ordre;
    private String codeImplementation;

    /** Icône du bouton d'action, en classe PrimeIcons. Voir {@link StepDecision#getIconeParDefaut()}. */
    private String icon;

    /** Couleur du bouton d'action. Voir {@link StepDecision#getSeveriteParDefaut()}. */
    private SeveriteAction severite;

    public TransitionPersistante() {
        super();
    }

    public TransitionPersistante(final String pCode, final Etat pEtatOrigine, final Etat pEtatDestination) {
        super(pCode, pEtatOrigine, pEtatDestination);
    }

    public boolean estRestreinte() {
        return this.permission != null && !this.permission.isBlank();
    }
}
