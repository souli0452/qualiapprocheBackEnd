package com.qualiapproche.workflow.persistence.model;

import com.qualiapproche.workflow.core.model.Etat;
import com.qualiapproche.workflow.core.model.Transition;
import com.qualiapproche.workflow.model.SeveriteAction;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

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

    /**
     * Identifiants des personnes qui co-signent l'étape d'où part cette transition, ou ensemble
     * vide.
     *
     * <p>Portés par la transition et non lus sur l'étape, pour la même raison que l'habilitation :
     * le moteur ne connaît que des transitions, et {@code WorkflowConditionAdapter} n'a pas accès
     * aux entités. C'est {@code WorkflowEngineDAOAdapter} qui les y recopie à la construction du
     * catalogue. Toutes les transitions d'une même étape en portent donc la même liste — la
     * séparation des signatures vaut pour l'étape, pas pour l'une de ses issues.</p>
     */
    private Set<String> cosignataires = Set.of();

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
