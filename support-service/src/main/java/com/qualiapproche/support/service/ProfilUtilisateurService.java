package com.qualiapproche.support.service;

import com.qualiapproche.common.utils.PermissionsPortee;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.support.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Structure de rattachement et rôles de l'utilisateur connecté, avec cache de courte durée.
 *
 * <p>La visibilité d'un document se décide sur ces deux éléments, et elle est évaluée à chaque
 * recherche, chaque consultation et chaque téléchargement : sans cache, un simple affichage de
 * liste déclencherait plusieurs appels à user-service. La structure et les rôles d'une personne
 * ne changeant qu'exceptionnellement, quelques dizaines de secondes de rétention suffisent à
 * absorber ces rafales tout en propageant rapidement un changement d'affectation.</p>
 *
 * <p><b>En cas d'indisponibilité de user-service, le profil est vide</b> : l'utilisateur ne voit
 * alors que ses propres documents et ceux partagés nommément avec lui. C'est délibéré — une panne
 * de résolution doit restreindre l'accès, jamais l'élargir.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilUtilisateurService {

    private final UserClient userClient;

    @Value("${support.profil.cache-seconds:60}")
    private long retentionSecondes;

    @Value("${support.profil.cache-taille-max:5000}")
    private int tailleMax;

    private final Map<String, Entree> cache = new ConcurrentHashMap<>();

    /**
     * Profil réduit à ce dont dépend la visibilité.
     *
     * <p>Il porte les <b>permissions</b> et non plus des noms de rôles. Les rôles se créent depuis
     * l'écran d'administration : une organisation nomme les siens, et un test sur
     * {@code "RESPONSABLE_QUALITE"} ou {@code "ADMIN"} ignorait tout d'un rôle équivalent portant
     * un autre nom — lequel se voyait borné à sa propre structure sans que rien ne l'explique.</p>
     *
     * <p>Les <b>noms de rôles</b> y restent, mais pour une seule chose : le classement des
     * documents compare aux rôles qu'un administrateur a inscrits sur un niveau de confidentialité.
     * Ces noms-là sont de la <i>donnée</i>, choisis dans l'écran et comparés à eux-mêmes — rien à
     * voir avec un nom écrit en dur dans le code, qui lui décidait à la place de l'administrateur.
     * Aucune décision d'habilitation ne se prend plus sur {@code roles}.</p>
     *
     * @param structureId structure de rattachement, ou {@code null} si l'utilisateur n'en a pas
     * @param roles       noms des rôles portés, pour la seule comparaison au classement documentaire
     * @param permissions permissions applicatives détenues, telles que user-service les sert
     */
    public record Profil(String structureId, Set<String> roles, Set<String> permissions) {

        public boolean detient(String permission) {
            return permissions.contains(permission);
        }

        /**
         * Voit-il l'ensemble des structures ?
         *
         * <p>Une seule permission le dit désormais, accordée depuis l'écran à qui de droit. Elle
         * remplace une liste de noms — {@code RESPONSABLE_QUALITE}, {@code SUPER_ADMIN},
         * {@code SUPERADMIN}, {@code ADMIN} — dont l'orthographe même trahissait la fragilité.</p>
         */
        public boolean voitToutesLesStructures() {
            return detient(PermissionsPortee.TOUTES_STRUCTURES);
        }

        /**
         * Relève-t-il de l'administration générale ?
         *
         * <p>Seule cette permission échappe au classement des documents. Voir toutes les structures
         * ne l'emporte pas : le responsable qualité voit toutes les structures, pas tous les
         * classements. La dispense existe pour qu'un document mal classé — sur un rôle que plus
         * personne ne détient — reste réparable ; sans elle, plus personne ne pourrait ni le voir
         * ni le reclasser.</p>
         */
        public boolean estAdministrateur() {
            return detient(PermissionsPortee.HORS_CLASSEMENT);
        }

        static Profil vide() {
            return new Profil(null, Set.of(), Set.of());
        }
    }

    private record Entree(Profil profil, Instant expiration) {
        boolean estPerimee() {
            return Instant.now().isAfter(expiration);
        }
    }

    public Profil profilCourant() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Profil.vide();
        }

        Entree entree = cache.get(userId);
        if (entree != null && !entree.estPerimee()) {
            return entree.profil();
        }

        Profil profil = interroger(userId);
        if (profil != null) {
            purgerSiNecessaire();
            cache.put(userId, new Entree(profil, Instant.now().plus(Duration.ofSeconds(retentionSecondes))));
            return profil;
        }

        // Appel en échec : la dernière valeur connue, même périmée, vaut mieux que de fermer
        // brutalement l'accès à qui consultait ses documents l'instant d'avant.
        if (entree != null) {
            log.warn("Profil de {} indisponible : réutilisation de la dernière valeur connue.", userId);
            return entree.profil();
        }
        return Profil.vide();
    }

    /**
     * Profil d'un autre utilisateur que l'appelant — consultation par un administrateur des
     * partages d'un tiers. Non mis en cache : le cas est rare, et le cache est dimensionné pour
     * les rafales de l'utilisateur courant.
     */
    public Profil profilDe(String userId) {
        if (userId == null || userId.isBlank()) {
            return Profil.vide();
        }
        Profil profil = interroger(userId);
        return profil != null ? profil : Profil.vide();
    }

    /** @return le profil, ou {@code null} si l'interrogation a échoué. */
    @SuppressWarnings("unchecked")
    private Profil interroger(String userId) {
        try {
            Map<String, Object> reponse = userClient.getUserById(userId);
            if (reponse == null) {
                return Profil.vide();
            }

            Object structure = reponse.get("structure");
            String structureId = (structure == null || structure.toString().isBlank())
                    ? null : structure.toString().trim();

            // Les permissions décident ; les noms de rôles ne servent plus qu'à se comparer aux
            // rôles qu'un administrateur a inscrits sur un niveau de confidentialité.
            Object roles = reponse.get("roles") != null ? reponse.get("roles") : reponse.get("appRoles");
            Set<String> nomsDeRoles = textes(roles, true);
            Set<String> permissions = textes(reponse.get("permissions"), false);

            return new Profil(structureId, nomsDeRoles, permissions);
        } catch (Exception e) {
            log.error("Profil de {} introuvable auprès de user-service : {}", userId, e.getMessage());
            return null;
        }
    }

    /** Les chaînes non vides d'une liste servie par user-service, éventuellement normalisées. */
    private static Set<String> textes(Object brut, boolean enMajuscules) {
        if (!(brut instanceof List<?> liste)) {
            return Set.of();
        }
        return liste.stream()
                .filter(java.util.Objects::nonNull)
                .map(v -> enMajuscules ? v.toString().trim().toUpperCase() : v.toString().trim())
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toSet());
    }

    private void purgerSiNecessaire() {
        if (cache.size() < tailleMax) {
            return;
        }
        cache.values().removeIf(Entree::estPerimee);
        if (cache.size() >= tailleMax) {
            log.info("Cache des profils saturé ({} entrées, toutes valides) : il est vidé.", cache.size());
            cache.clear();
        }
    }
}
