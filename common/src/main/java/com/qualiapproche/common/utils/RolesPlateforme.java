package com.qualiapproche.common.utils;

/**
 * Noms des rôles livrés par défaut, et habilitations qui ne désignent pas un rôle.
 *
 * <p><b>Aucune décision ne se prend plus sur ces noms.</b> Les listes {@code PORTEE_GLOBALE} et
 * {@code DECIDE_PARTOUT} qui vivaient ici ont été remplacées par les permissions de
 * {@link PermissionsPortee} : les rôles se créent depuis l'écran d'administration, chaque
 * installation nomme les siens, et un code qui teste {@code "RESPONSABLE_QUALITE"} refuse à un rôle
 * « Coordonnateur qualité » créé le lendemain un droit que sa fonction suppose — sans que rien ne
 * l'explique.</p>
 *
 * <p>Ce qui subsiste ici ne décide de rien : les deux noms servent à <b>semer</b> les rôles par
 * défaut d'une installation neuve, et les deux habilitations désignent une personne — le titulaire,
 * le créateur — là où un rôle ne saurait pas le dire.</p>
 */
public final class RolesPlateforme {

    /**
     * Administration technique de la plateforme.
     *
     * <p>Nom du rôle semé à l'installation, non un critère de décision : rien n'empêche une
     * organisation de le renommer ou d'en créer d'autres. Ce qu'il peut faire tient aux permissions
     * qu'il détient, pas à son nom.</p>
     */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** Responsabilité du système qualité. Semé à l'installation, comme {@link #SUPER_ADMIN}. */
    public static final String RESPONSABLE_QUALITE = "RESPONSABLE_QUALITE";

    /**
     * Habilitation qui ne désigne pas un rôle, mais la personne à qui le dossier a été confié.
     *
     * <p>Certaines étapes ne se décident pas par appartenance à un groupe : l'agent à qui une
     * non-conformité est imputée traite <b>la sienne</b>. Nommer cela « agent imputé » et en faire
     * un rôle ouvrait l'étape à tous ceux qui peuvent être imputés, sur tous les dossiers.</p>
     *
     * <p>Le préfixe la distingue d'un nom de rôle, qu'aucun ne peut porter. Elle s'écrit là où
     * s'écrirait un rôle — habilitation d'une transition, rôle responsable d'une étape ou de son
     * modèle au catalogue — de sorte que la chaîne de résolution existante la transporte sans
     * traitement particulier.</p>
     */
    public static final String HABILITATION_TITULAIRE = "@TITULAIRE";

    /**
     * Habilitation qui désigne la personne ayant <b>ouvert</b> le dossier — celle qui a déclaré la
     * non-conformité, déposé le document, formé la demande.
     *
     * <p>Distincte du titulaire, et c'est tout l'intérêt : le titulaire se déplace au fil du
     * circuit — l'imputation le réinscrit — tandis que le créateur ne change jamais. Une étape que
     * le dossier peut <b>retraverser</b> ne peut se fonder que sur lui : réservée au titulaire, elle
     * n'ouvrirait plus à personne dès que l'imputation a désigné quelqu'un d'autre, et un dossier
     * renvoyé à son auteur resterait immobile.</p>
     *
     * <p>Elle exprime ce qu'un rôle ne sait pas dire : soumettre est un acte personnel, alors que
     * {@code AGENT} est un groupe. Réservée à un rôle, l'étape de soumission ouvrait le brouillon de
     * chacun à tous les autres.</p>
     *
     * <p>Le circuit livré s'en sert, mais rien dans le code ne nomme d'étape : c'est l'habilitation
     * écrite sur l'étape ou sur la transition qui décide, et l'éditeur la propose comme un rôle.</p>
     */
    public static final String HABILITATION_CREATEUR = "@CREATEUR";

    private RolesPlateforme() {
    }
}
