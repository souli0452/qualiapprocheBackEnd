package com.qualiapproche.workflow.model;

public enum StepDecision {

    APPROUVE("pi pi-check", SeveriteAction.SUCCESS),
    REJETE("pi pi-times", SeveriteAction.DANGER);

    private final String iconeParDefaut;
    private final SeveriteAction severiteParDefaut;

    StepDecision(final String pIcone, final SeveriteAction pSeverite) {
        this.iconeParDefaut = pIcone;
        this.severiteParDefaut = pSeverite;
    }

    /**
     * Icône présentée quand la transition n'en déclare aucune.
     *
     * <p>Les circuits créés avant que l'apparence des actions ne soit configurable n'ont ni icône
     * ni couleur. Les laisser vides aurait rendu leurs boutons indistincts ; la décision suffit à
     * en déduire une présentation juste, qu'une valeur explicite remplace dès qu'elle est saisie.</p>
     */
    public String getIconeParDefaut() {
        return iconeParDefaut;
    }

    /** Couleur présentée quand la transition n'en déclare aucune. Voir {@link #getIconeParDefaut()}. */
    public SeveriteAction getSeveriteParDefaut() {
        return severiteParDefaut;
    }
}
