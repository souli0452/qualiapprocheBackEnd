package com.qualiapproche.workflow.service;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.client.UserRoleClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structure de rattachement d'un utilisateur, avec cache de courte durée.
 *
 * <p>La structure vit en attribut Keycloak, que user-service sait lire. Le jeton, lui, ne la
 * porte que si le royaume Keycloak mappe cet attribut en claim {@code structure_id} — et ce
 * mappage n'est configuré nulle part dans le projet : {@code SecurityUtils.getCurrentUserStructureId()}
 * rendait donc {@code null} pour tout le monde. Or c'est cette valeur qui borne les notifications
 * et les décisions d'étape à la structure du dossier : sans elle, l'instance s'ouvrait sans
 * structure et le filtrage — silencieusement — ne s'appliquait jamais.</p>
 *
 * <p>Le jeton reste lu en premier : si le mappage est un jour configuré, aucun appel réseau
 * n'est nécessaire. À défaut, la structure est demandée à sa source.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructureUtilisateurService {

    private final UserRoleClient userRoleClient;

    @Value("${workflow.structures.cache-seconds:60}")
    private long retentionSecondes;

    @Value("${workflow.structures.cache-taille-max:5000}")
    private int tailleMax;

    private final Map<String, Entree> cache = new ConcurrentHashMap<>();

    /** L'absence de structure se met en cache aussi : elle coûte le même appel qu'une présence. */
    private record Entree(String structureId, Instant expiration) {
        boolean estPerimee() {
            return Instant.now().isAfter(expiration);
        }
    }

    /**
     * Structure de l'appelant : celle du jeton, à défaut celle que user-service lit dans son
     * profil Keycloak.
     *
     * @return l'identifiant de structure, ou {@code null} si l'utilisateur n'en a pas — ou n'est
     *         pas identifiable
     */
    public String structureDeLUtilisateurCourant() {
        String duJeton = SecurityUtils.getCurrentUserStructureId();
        if (duJeton != null) {
            return duJeton;
        }
        return structureDe(SecurityUtils.getCurrentUserId());
    }

    /**
     * Structure d'un utilisateur désigné, lue chez user-service.
     *
     * @return l'identifiant de structure, ou {@code null} si l'utilisateur n'en a pas ou si
     *         user-service est injoignable sans valeur en cache
     */
    public String structureDe(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        String cle = userId.trim();

        Entree entree = cache.get(cle);
        if (entree != null && !entree.estPerimee()) {
            return entree.structureId();
        }

        try {
            Map<String, Object> reponse = userRoleClient.getUserById(cle);
            Object donnees = reponse != null ? reponse.getOrDefault("data", reponse) : null;
            String structure = null;
            if (donnees instanceof Map<?, ?> utilisateur) {
                Object valeur = utilisateur.get("structure");
                structure = valeur == null || valeur.toString().isBlank()
                        ? null : valeur.toString().trim();
            }
            purgerSiNecessaire();
            cache.put(cle, new Entree(structure,
                    Instant.now().plus(Duration.ofSeconds(retentionSecondes))));
            return structure;
        } catch (Exception e) {
            log.warn("Structure de l'utilisateur {} indisponible auprès de user-service : {}",
                    cle, e.getMessage());
            // La dernière valeur connue, même périmée, vaut mieux qu'un dossier sans structure.
            return entree != null ? entree.structureId() : null;
        }
    }

    /** Évacue les entrées périmées lorsque le cache atteint sa borne, et le vide si cela ne suffit pas. */
    private void purgerSiNecessaire() {
        if (cache.size() < tailleMax) {
            return;
        }
        cache.values().removeIf(Entree::estPerimee);
        if (cache.size() >= tailleMax) {
            cache.clear();
        }
    }
}
