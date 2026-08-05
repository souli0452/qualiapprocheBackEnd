package com.qualiapproche.common.enumeration;

/**
 * États de traitement d'un dossier, dans l'ordre où le circuit les fait franchir.
 *
 * <p>Une étape dont l'état ne figure pas ici est franchie par le moteur mais laisse le dossier sur
 * son état précédent : le module métier ne sait pas la nommer. Toute étape ajoutée à un circuit
 * doit donc trouver ici son état.</p>
 */
public enum Etat {

    SOUMISSION,
    RECEPTION,
    /**
     * Validation du responsable qualité, qui désigne la structure à qui le dossier est confié.
     *
     * <p>Charnière du circuit : jusqu'à elle le dossier n'appartient à personne, après elle il est
     * adressé à une structure dont le pilote l'imputera. C'est là, et là seulement, que se décide
     * sa destination.</p>
     */
    VALIDATION_RQ,
    IMPUTATION,
    TRAITEMENT,
    VALIDATION,
    VALIDATION_RS,
    SUIVI_RQ,
    CLOTURE
}
