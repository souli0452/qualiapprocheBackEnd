package com.qualiapproche.referentiel.entities;

/** Nature de la valeur d'un {@link Parametre}, pour la présenter et la vérifier. */
public enum TypeParametre {
    /** Texte libre : un nom d'organisation, une mention légale. */
    TEXTE,
    /** Adresse de courriel. */
    COURRIEL,
    /** Numéro de téléphone. */
    TELEPHONE,
    /** Adresse web. */
    URL,
    /** Adresse d'une image, affichée telle quelle — un logo. */
    IMAGE,
    /**
     * Nombre entier — un délai en jours, un seuil.
     *
     * <p>Vérifié à l'enregistrement : ce qui lit un tel réglage le convertit, et une valeur non
     * numérique n'échouerait qu'au moment de s'en servir, loin de qui l'a saisie.</p>
     */
    NOMBRE,
    /** Adresse postale, éventuellement sur plusieurs lignes. */
    ADRESSE
}
