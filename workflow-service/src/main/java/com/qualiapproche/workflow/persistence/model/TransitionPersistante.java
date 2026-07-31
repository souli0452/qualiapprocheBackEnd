package com.qualiapproche.workflow.persistence.model;

import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.Transition;
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
    private Integer ordre;
    private String codeImplementation;
    private String icon;

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
