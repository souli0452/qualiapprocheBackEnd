package com.qualiapproche.amelioration.utils;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.common.cloisonnement.Cloisonnement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Réglages de l'organisation, tels que referentiel-service les détient.
 *
 * <p>Ils ont remplacé l'ancienne configuration globale, dont les trois champs étaient figés dans le
 * code : le nom et le courriel du responsable qualité, et le délai de rappel avant échéance. Ce
 * module en lit deux — la copie au responsable qualité et le seuil de relance des plans d'action.</p>
 *
 * <p>Mis en cache, client par client : la tournée de nuit des plans d'action interrogerait le référentiel une fois par
 * plan pour une donnée qui change deux fois par an. Le référentiel injoignable ne fait échouer aucun
 * envoi : la dernière carte connue est rendue, et à défaut la valeur par défaut de l'appelant.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReglagesOrganisation {

    private final ReferentielClient referentielClient;

    @Value("${amelioration.reglages.cache-secondes:600}")
    private long retentionSecondes;

    /** Les réglages lus, par client : chacun a les siens, et l'un ne doit pas servir à l'autre. */
    private record Lecture(Map<String, String> reglages, Instant peremption) {
    }

    private final Map<UUID, Lecture> parDirection = new HashMap<>();

    /** Valeur d'un réglage, ou {@code null} s'il n'est pas renseigné. */
    public String valeur(String cle) {
        String valeur = valeurs().get(cle);
        return valeur != null && !valeur.isBlank() ? valeur : null;
    }

    /**
     * Réglage lu comme un nombre entier.
     *
     * <p>Le référentiel refuse déjà une valeur non numérique sur un réglage de nature « nombre ».
     * Une valeur illisible malgré tout — réglage dont la nature a changé — rend le défaut plutôt que
     * de faire échouer la tournée de rappels : mieux vaut relancer au délai habituel que ne relancer
     * personne.</p>
     *
     * @param cle    clé du réglage
     * @param defaut valeur retenue si le réglage est vide, absent ou illisible
     */
    public long entier(String cle, long defaut) {
        String valeur = valeur(cle);
        if (valeur == null) {
            return defaut;
        }
        try {
            return Long.parseLong(valeur.trim());
        } catch (NumberFormatException e) {
            log.warn("Le réglage « {} » ne contient pas un nombre (« {} ») : {} est retenu.",
                    cle, valeur, defaut);
            return defaut;
        }
    }

    private synchronized Map<String, String> valeurs() {
        UUID direction = Cloisonnement.courante();
        Lecture connue = parDirection.get(direction);
        if (connue != null && Instant.now().isBefore(connue.peremption())) {
            return connue.reglages();
        }
        Map<String, String> precedents = connue == null ? Map.of() : connue.reglages();
        try {
            Map<String, String> lues = new LinkedHashMap<>();
            Map<String, String> reponse = referentielClient.parametresPublics();
            if (reponse != null) {
                reponse.forEach((cle, valeur) -> {
                    if (cle != null && valeur != null && !valeur.isBlank()) {
                        lues.put(cle, valeur.trim());
                    }
                });
            }
            parDirection.put(direction, new Lecture(lues,
                    Instant.now().plus(Duration.ofSeconds(retentionSecondes))));
            return lues;
        } catch (Exception e) {
            // La péremption est raccourcie pour que le référentiel revenu soit repris sans attendre
            // la fenêtre complète.
            log.warn("Réglages de l'organisation indisponibles : {}", e.getMessage());
            parDirection.put(direction, new Lecture(precedents,
                    Instant.now().plus(Duration.ofSeconds(Math.min(retentionSecondes, 60)))));
            return precedents;
        }
    }
}
