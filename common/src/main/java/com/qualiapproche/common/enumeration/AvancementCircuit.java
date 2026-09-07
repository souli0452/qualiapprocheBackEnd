package com.qualiapproche.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Où en est un dossier dans son circuit, dit en trois mots et rien de plus.
 *
 * <p>C'est la seule réponse dont un tableau de bord a besoin : combien de dossiers sont ouverts,
 * combien sont finis, combien n'ont jamais été soumis. La déduire de l'état de traitement du
 * dossier — {@code CLOTURE}, {@code DRAFT} — revenait à tenir dans chaque module une seconde table
 * de règles, qui cessait d'être exacte à la première étape ajoutée au circuit et qui mentait pour
 * tout circuit dont les étapes ne portent pas ces noms-là.</p>
 *
 * <p>Le moteur, lui, n'a rien à interpréter : une instance terminée est terminée, une instance qui
 * n'a franchi aucune étape n'a jamais été soumise.</p>
 */
@Schema(description = "Où en est un dossier dans son circuit. Établi par le moteur seul, sans "
        + "égard au nom des étapes : un circuit remanié ne change donc pas la lecture.")
public enum AvancementCircuit {

    /**
     * Le circuit n'a rien enregistré : aucune instance, ou une instance ouverte sur laquelle
     * personne n'a encore rien décidé — le dossier est resté chez son auteur.
     */
    NON_ENGAGE,

    /** Le circuit court : une décision au moins a été prise, et le dossier n'est pas arrivé. */
    EN_COURS,

    /** Le circuit a rendu son verdict. Il ne dit pas lequel : approbation, rejet ou clôture. */
    TERMINE
}
