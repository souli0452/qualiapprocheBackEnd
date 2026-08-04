package com.qualiapproche.workflow.service;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.client.UserRoleClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rôles de l'utilisateur connecté, avec cache de courte durée.
 *
 * <p>Le contrôle d'habilitation est évalué à chaque lecture d'état et pour chaque transition :
 * sans cache, une consultation d'écran déclencherait plusieurs appels à user-service. Les rôles
 * d'un utilisateur ne changeant qu'exceptionnellement, une rétention de quelques dizaines de
 * secondes suffit à absorber ces rafales tout en propageant rapidement une révocation.</p>
 *
 * <p>Chaque entrée retient à la fois les identifiants et les noms des rôles : les circuits
 * désignent le rôle responsable tantôt par l'un, tantôt par l'autre.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RolesUtilisateurService {

    private final UserRoleClient userRoleClient;

    @Value("${workflow.roles.cache-seconds:60}")
    private long retentionSecondes;

    /**
     * Nombre d'utilisateurs retenus simultanément.
     *
     * <p>Le cache n'était jamais purgé : une entrée par utilisateur vu depuis le démarrage y
     * restait indéfiniment, y compris pour ceux qui ne reviendraient jamais. Sur un service de
     * longue durée, la table ne pouvait que croître.</p>
     */
    @Value("${workflow.roles.cache-taille-max:5000}")
    private int tailleMax;

    private final Map<String, Entree> cache = new ConcurrentHashMap<>();

    private record Entree(Set<String> roles, Instant expiration) {
        boolean estPerimee() {
            return Instant.now().isAfter(expiration);
        }
    }

    /**
     * Identifiants <b>et</b> noms des rôles de l'utilisateur courant, en majuscules.
     * Ensemble vide si l'utilisateur n'est pas identifiable ou si user-service est injoignable —
     * auquel cas aucune transition restreinte ne sera autorisée.
     */
    public Set<String> rolesDeLUtilisateurCourant() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Set.of();
        }

        Entree entree = cache.get(userId);
        if (entree != null && !entree.estPerimee()) {
            return entree.roles();
        }

        Set<String> roles = interroger(userId);
        if (roles != null) {
            purgerSiNecessaire();
            cache.put(userId, new Entree(roles, Instant.now().plus(Duration.ofSeconds(retentionSecondes))));
            return roles;
        }

        // Appel en échec : on se rabat sur la dernière valeur connue, même périmée, plutôt que de
        // bloquer l'utilisateur sur une indisponibilité passagère.
        if (entree != null) {
            log.warn("Rôles de {} indisponibles : réutilisation de la dernière valeur connue.", userId);
            return entree.roles();
        }
        return Set.of();
    }

    /**
     * Évacue les entrées périmées lorsque le cache atteint sa borne, et le vide entièrement si
     * cela ne suffit pas.
     *
     * <p>Une purge complète n'a d'autre effet qu'un surcroît d'appels à user-service le temps que
     * le cache se reconstitue : aucune décision ne s'appuie sur son contenu au-delà de la
     * fraîcheur déjà garantie par l'expiration.</p>
     */
    private void purgerSiNecessaire() {
        if (cache.size() < tailleMax) {
            return;
        }
        cache.values().removeIf(Entree::estPerimee);
        if (cache.size() >= tailleMax) {
            log.info("Cache des rôles saturé ({} entrées, toutes valides) : il est vidé.", cache.size());
            cache.clear();
        }
    }

    /** @return les rôles, ou {@code null} si l'interrogation a échoué. */
    @SuppressWarnings("unchecked")
    private Set<String> interroger(String userId) {
        try {
            Map<String, Object> reponse = userRoleClient.getMyRoles();
            Object data = reponse != null ? reponse.get("data") : null;
            if (!(data instanceof List<?> liste)) {
                log.warn("Réponse inattendue de user-service pour les rôles de {}.", userId);
                return Set.of();
            }

            Set<String> roles = new HashSet<>();
            for (Object element : liste) {
                if (element instanceof Map<?, ?> role) {
                    ajouter(roles, role.get("id"));
                    ajouter(roles, role.get("name"));
                }
            }
            return roles;
        } catch (Exception e) {
            log.error("Impossible de récupérer les rôles de {} auprès de user-service : {}",
                    userId, e.getMessage());
            return null;
        }
    }

    private void ajouter(Set<String> roles, Object valeur) {
        if (valeur != null && !valeur.toString().isBlank()) {
            roles.add(valeur.toString().trim().toUpperCase());
        }
    }
}
