package com.qualiapproche.common.enumeration;


/**
 * Modules qu'une direction peut souscrire. La licence en porte la liste, séparée par des
 * virgules, et c'est cette liste — et elle seule — qui ouvre un pan de l'application : une
 * permission détenue sur un module non souscrit ne donne accès à rien.
 *
 * <p>Ces valeurs sont donc un contrat, partagé avec le front (constante {@code Modules}) et
 * inscrit tel quel dans les licences émises. Un nom qui n'y figure pas ne peut être souscrit
 * par personne — le front gardait ainsi quatre entrées derrière un « AUTRE_MODULE » inexistant,
 * invisibles pour tous, y compris l'administrateur.</p>
 */
public enum ModuleAbonnement {
    NON_CONFORMITE,
    /** Gestion documentaire : documents qualité, types de document, versions et circuits associés. */
    DOCUMENTAIRE,
    RECLAMATION,
    RISQUE,
    AUDIT,
    FORMATION,
    REGLEMENTATION,
    EVALUATION,
    CONTEXTE
}
// Le plan d'action ne se souscrit pas : il relève du traitement des non-conformités, et donc du
// module NON_CONFORMITE. Un module PLAN_ACTION figurait ici, qu'aucune licence n'a jamais porté ;
// il ne faut pas le confondre avec la famille de ressource du même nom
// (TypeRessource.PLAN_ACTION), qui désigne le service auquel un
// circuit rend ses décisions — une tout autre notion.
