package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.client.ParametreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Réglages de l'organisation, tels que referentiel-service les détient : nom, contact, logo,
 * responsable qualité.
 *
 * <p>Un seul lecteur pour tout le service : le pied de page des courriels et la mise en copie du
 * responsable qualité s'appuient sur les mêmes valeurs, et un courriel par destinataire d'une étape
 * ferait autant d'appels au référentiel pour une donnée qui change deux fois par an.</p>
 *
 * <p>Le référentiel injoignable n'empêche jamais un envoi : la dernière carte connue est rendue,
 * éventuellement vide, et la prochaine tentative reprendra. Un message sans signature vaut mieux
 * qu'un responsable jamais prévenu.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReglagesOrganisation {

    private final ParametreClient parametreClient;

    @Value("${workflow.notifications.reglages.cache-secondes:600}")
    private long retentionSecondes;

    private Map<String, String> reglages = Map.of();
    private Instant peremption = Instant.EPOCH;

    /** Réglages renseignés, indexés par clé. Jamais {@code null}, parfois vide. */
    public synchronized Map<String, String> valeurs() {
        if (Instant.now().isBefore(peremption)) {
            return reglages;
        }
        try {
            Map<String, Object> reponse = parametreClient.valeursPubliques();
            Object donnees = reponse != null ? reponse.get("data") : null;
            Map<String, String> lues = new LinkedHashMap<>();
            if (donnees instanceof Map<?, ?> carte) {
                carte.forEach((cle, valeur) -> {
                    if (cle != null && valeur != null && !valeur.toString().isBlank()) {
                        lues.put(cle.toString(), valeur.toString().trim());
                    }
                });
            }
            reglages = lues;
            peremption = Instant.now().plus(Duration.ofSeconds(retentionSecondes));
        } catch (Exception e) {
            // Rien ici ne doit interrompre un envoi. La péremption est raccourcie pour que le
            // référentiel revenu soit repris sans attendre la fenêtre complète.
            log.warn("Réglages de l'organisation indisponibles : {}", e.getMessage());
            peremption = Instant.now().plus(Duration.ofSeconds(Math.min(retentionSecondes, 60)));
        }
        return reglages;
    }

    /**
     * Valeur d'un réglage, ou {@code null} s'il n'est pas renseigné.
     *
     * @param cle clé du réglage, telle que {@code ClesReglages} la nomme
     */
    public String valeur(String cle) {
        String valeur = valeurs().get(cle);
        return valeur != null && !valeur.isBlank() ? valeur : null;
    }
}
