package com.qualiapproche.workflow.service;

import com.qualiapproche.common.utils.ClesReglages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Mise en copie du responsable qualité, obligatoire pour ce qui sort d'une non-conformité.
 *
 * <p>Le responsable qualité pilote le traitement des non-conformités : il doit voir passer ce que
 * le circuit envoie à leur sujet, sans qu'on pense à l'ajouter destinataire par destinataire dans
 * chaque étape de chaque circuit. La règle est donc portée par le code, à l'unique point de remise
 * des courriels, et non par la configuration — où elle serait à refaire à chaque circuit créé.</p>
 *
 * <p>Les plans d'action en relèvent aussi : un plan naît d'une non-conformité et son avancement est
 * l'avancement du traitement de celle-ci.</p>
 *
 * <p>Son adresse est un réglage de l'organisation ({@code RESPONSABLE_QUALITE_EMAIL}). Non
 * renseignée, le message part <b>sans</b> copie : priver un responsable de sa notification parce que
 * le référentiel est incomplet serait un remède pire que le mal. Le journal le signale, pour que le
 * réglage soit renseigné.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CopieAuResponsableQualite {

    /**
     * Types de ressource relevant du module des non-conformités.
     *
     * <p>Comparés sans tenir compte de la casse, comme partout où le type de ressource circule en
     * chaîne dans les notifications.</p>
     */
    private static final Set<String> RESSOURCES_DU_MODULE_NC = Set.of("NON_CONFORMITE", "PLAN_ACTION");

    private final ReglagesOrganisation reglages;

    /**
     * Adresse à mettre en copie pour un courriel concernant cette ressource.
     *
     * @param typeRessource type porté par la notification ({@code NON_CONFORMITE}, {@code DOCUMENT}…)
     * @return l'adresse du responsable qualité si la ressource relève des non-conformités et que
     *         l'adresse est renseignée ; {@code null} sinon — un document ou une demande n'a pas à
     *         lui être copié
     */
    public String pour(String typeRessource) {
        if (typeRessource == null
                || !RESSOURCES_DU_MODULE_NC.contains(typeRessource.trim().toUpperCase(Locale.ROOT))) {
            return null;
        }
        String courriel = reglages.valeur(ClesReglages.RESPONSABLE_QUALITE_EMAIL);
        if (courriel == null) {
            log.warn("Le responsable qualité doit être en copie des courriels de non-conformité, mais "
                    + "le réglage « {} » n'est pas renseigné : le message part sans copie.",
                    ClesReglages.RESPONSABLE_QUALITE_EMAIL);
        }
        return courriel;
    }
}
