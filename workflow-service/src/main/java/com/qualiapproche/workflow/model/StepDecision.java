package com.qualiapproche.workflow.model;

public enum StepDecision {

    APPROUVE("pi pi-check", SeveriteAction.SUCCESS),
    REJETE("pi pi-times", SeveriteAction.DANGER),

    /**
     * Met le dossier en clôture : ni un pas en avant, ni un retour en arrière, mais la fin de son
     * parcours.
     *
     * <p>Avant elle, clore se disait {@code APPROUVE} — « Clôturer la NC » approuvait — et l'issue
     * publiée aux modules métier confondait le dossier clos avec le dossier validé. La clôture est
     * une nature à part entière : le bouton porte sa propre couleur, et la fin de circuit se
     * publie {@code CLOSED} au lieu d'usurper {@code APPROVED} ou {@code REJECTED}.</p>
     */
    CLOTURE("pi pi-lock", SeveriteAction.WARN);

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
