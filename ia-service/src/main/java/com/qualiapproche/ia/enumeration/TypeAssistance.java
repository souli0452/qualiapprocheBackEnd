package com.qualiapproche.ia.enumeration;

/**
 * Nature de l'assistance rédactionnelle demandée. Chaque type désigne un prompt système dédié
 * du {@code PromptRegistry} : le ton et la structure attendus d'une description de non-conformité
 * ne sont pas ceux d'une reformulation, et un prompt unique lisserait tout.
 */
public enum TypeAssistance {
    /** Rédiger la description d'une non-conformité à partir des notes de son déclarant. */
    DESCRIPTION_NON_CONFORMITE,
    /** Proposer des pistes de causes pour un plan d'action. */
    CAUSES_PLAN_ACTION,
    /** Proposer des pistes de solutions pour un plan d'action. */
    SOLUTIONS_PLAN_ACTION,
    /** Rédiger le contenu d'un document du système qualité. */
    CONTENU_DOCUMENT_QMS,
    /** Reformuler un texte existant dans un langage qualité professionnel. */
    REFORMULATION,
    /** Assistance libre, sans structure attendue particulière. */
    TEXTE_LIBRE
}
