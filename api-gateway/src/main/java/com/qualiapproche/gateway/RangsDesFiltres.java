package com.qualiapproche.gateway;

import org.springframework.core.Ordered;

/**
 * L'ordre des filtres web de la passerelle, réuni en un seul endroit.
 *
 * <p>Ces filtres s'appuient les uns sur les autres, et leur ordre ne se devine pas en lisant
 * chacun d'eux :</p>
 * <ol>
 *   <li><b>en-têtes de sécurité</b> — le premier, pour que <i>toute</i> réponse les porte, refus
 *       des filtres suivants compris ;</li>
 *   <li><b>anti-CSRF</b> — avant CORS, pour qu'une origine étrangère reçoive un refus motivé en
 *       JSON plutôt que le 403 muet du filtre CORS ;</li>
 *   <li><b>CORS</b> — répond aux contrôles préalables et pose les en-têtes d'origine ;</li>
 *   <li><b>limitation de débit</b> — après CORS, sans quoi le navigateur masquerait le 429 et le
 *       frontal croirait à une panne réseau.</li>
 * </ol>
 *
 * <p>Tous passent avant le routage de Spring Cloud Gateway, qui n'est lui-même atteint qu'au bout
 * de la chaîne des filtres web.</p>
 */
final class RangsDesFiltres {

    static final int EN_TETES_DE_SECURITE = Ordered.HIGHEST_PRECEDENCE;
    static final int ANTI_CSRF = Ordered.HIGHEST_PRECEDENCE + 10;
    static final int CORS = Ordered.HIGHEST_PRECEDENCE + 20;
    static final int LIMITE_DE_DEBIT = Ordered.HIGHEST_PRECEDENCE + 30;

    private RangsDesFiltres() {
    }
}
