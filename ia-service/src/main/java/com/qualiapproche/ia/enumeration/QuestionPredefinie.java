package com.qualiapproche.ia.enumeration;

import com.qualiapproche.common.utils.PermissionsTableauDeBord;

/**
 * Les questions que l'assistant sait poser à l'API, et elles seules.
 *
 * <p>Une liste <b>close</b>, et c'est tout l'intérêt. Laisser le modèle choisir lui-même quel
 * point d'entrée appeler à partir d'une phrase libre demande un modèle que nous n'avons pas — un
 * 7B choisit mal et invente ses paramètres — et ouvrirait la porte à un texte piégé qui lui
 * dicterait l'appel. Ici c'est l'application qui décide, toujours juste ; le modèle ne fait que
 * commenter ce qu'on lui tend.</p>
 *
 * <p>Les quatre premières répondent à la même question — « qu'est-ce que je dois faire ? » —, une
 * fois par module. C'est délibéré : c'est la première chose qu'on demande à un assistant, et le
 * produit y répond aujourd'hui en quatre écrans qu'il faut penser à ouvrir. Toutes s'appuient sur
 * le même idiome {@code /a-traiter}, où c'est le circuit qui désigne les dossiers — jamais un
 * filtre d'état, qui rendrait ceux des autres structures.</p>
 */
public enum QuestionPredefinie {

    /** Les non-conformités sur lesquelles l'appelant peut décider. */
    CE_QUI_M_ATTEND(
            "Qu'est-ce qui m'attend ?",
            "Les non-conformités sur lesquelles vous pouvez décider maintenant."),

    /** Les plans d'action dont l'appelant a la charge. */
    MES_PLANS_ACTION(
            "Mes plans d'action",
            "Les plans d'action sur lesquels vous devez agir."),

    /** Les documents qualité en attente de sa décision. */
    DOCUMENTS_A_VISER(
            "Documents à viser",
            "Les documents qualité qui attendent votre validation."),

    /** Les demandes documentaires qu'il doit instruire. */
    DEMANDES_A_INSTRUIRE(
            "Demandes à instruire",
            "Les demandes de modification ou de suppression de document qui vous reviennent."),

    /**
     * Les chiffres de l'organisation entière.
     *
     * <p>Les trois questions de statistiques reprennent les habilitations que le module exige
     * déjà pour ses tableaux de bord — une par portée. Nommer ici la même chose autrement, ou
     * s'appuyer sur la portée de lecture des dossiers, aurait proposé des questions que le module
     * refuse ensuite : un catalogue qui promet ce qu'il ne tient pas est pire qu'un catalogue
     * court. Ces habilitations s'ajoutent à {@code nc-read} plutôt qu'elles ne s'y substituent :
     * voir un dossier et en compter l'ensemble ne sont pas le même droit.</p>
     */
    CHIFFRES_DE_L_ORGANISATION(
            "Chiffres de l'organisation",
            "Les non-conformités de toutes les structures : en cours, clôturées, en retard.",
            PermissionsTableauDeBord.ORGANISME),

    /** Les chiffres de la structure de rattachement de l'appelant. */
    CHIFFRES_DE_MA_STRUCTURE(
            "Chiffres de ma structure",
            "Les non-conformités de votre structure : en cours, clôturées, en retard.",
            PermissionsTableauDeBord.STRUCTURE),

    /** Ce que l'appelant a déclaré ou ce qui lui est imputé. */
    MES_CHIFFRES(
            "Mes chiffres",
            "Vos non-conformités : déclarées, imputées, en cours, clôturées.",
            PermissionsTableauDeBord.PERSONNEL);

    private final String libelle;
    private final String description;

    /**
     * Habilitation exigée pour que la question soit <b>proposée et servie</b>, ou {@code null}
     * quand elle ne demande rien de particulier.
     *
     * <p>Elle ne sert pas qu'à masquer une pastille : le service la vérifie avant de répondre.
     * Un catalogue filtré n'est pas une protection — l'adresse de la question se devine, et rien
     * n'empêche de la poster directement.</p>
     */
    private final String porteeRequise;

    QuestionPredefinie(String libelle, String description) {
        this(libelle, description, null);
    }

    QuestionPredefinie(String libelle, String description, String porteeRequise) {
        this.libelle = libelle;
        this.description = description;
        this.porteeRequise = porteeRequise;
    }

    public String getPorteeRequise() {
        return porteeRequise;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getDescription() {
        return description;
    }
}
