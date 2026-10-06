package com.qualiapproche.evaluation.controller;

/**
 * Les chemins du module d'audit.
 *
 * <p>Ici et non dans {@code ApiUrls} de common : ils ne servent qu'à ce service, et les y mettre
 * obligerait chaque module à reconstruire common pour un chemin qu'il n'appelle pas. Ils suivent
 * la convention du dépôt — {@code /all}, {@code /get/{id}}, {@code /create},
 * {@code /update/{id}}, {@code /delete/{id}} — qu'attend le service de pagination du front.</p>
 */
public final class AuditUrls {

    public static final String RACINE = "/api/v1";

    public static final String AUDITS = RACINE + "/audits";
    public static final String AUDITEURS = RACINE + "/auditeurs";
    public static final String CONSTATS = RACINE + "/constats";
    public static final String CHECKLISTS = RACINE + "/checklists-audit";
    public static final String REFERENTIEL_RQAPBF = RACINE + "/referentiel-rqapbf";
    public static final String EVALUATIONS_RQAPBF = RACINE + "/evaluations-rqapbf";
    public static final String EVALUATIONS_AUDITEUR = RACINE + "/evaluations-auditeur";

    public static final String TYPES_AUDIT = RACINE + "/types-audit";
    public static final String SITES = RACINE + "/sites-audit";
    public static final String TYPES_CONSTAT = RACINE + "/types-constat";
    public static final String CRITERES_EVALUATION_AUDITEUR = RACINE + "/criteres-evaluation-auditeur";
    public static final String NIVEAUX_EVALUATION_AUDITEUR = RACINE + "/niveaux-evaluation-auditeur";
    public static final String REGLES_NOTIFICATION = RACINE + "/regles-notification-audit";
    public static final String CODIFICATIONS = RACINE + "/codifications-document";

    private AuditUrls() {
    }
}
