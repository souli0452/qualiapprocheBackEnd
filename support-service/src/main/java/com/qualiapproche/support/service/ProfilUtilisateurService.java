package com.qualiapproche.support.service;

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

    /** Rôle qui voit l'ensemble des documents soumis, toutes structures confondues. */
    public static final String RESPONSABLE_QUALITE = "RESPONSABLE_QUALITE";

    /**
     * Rôles d'administration générale, qui voient également tout.
     *
     * <p>{@code SUPER_ADMIN} y figure sous ses deux orthographes : user-service accepte les deux
     * et normalise vers la première, mais un compte affecté avant cette normalisation peut porter
     * l'autre.</p>
     */
    private static final Set<String> ROLES_ADMINISTRATION =
            Set.of("SUPER_ADMIN", "SUPERADMIN", "ADMIN");

    private final UserClient userClient;

    @Value("${support.profil.cache-seconds:60}")
    private long retentionSecondes;

    @Value("${support.profil.cache-taille-max:5000}")
    private int tailleMax;

    private final Map<String, Entree> cache = new ConcurrentHashMap<>();

    /**
     * Profil réduit à ce dont dépend la visibilité.
     *
     * @param structureId structure de rattachement, ou {@code null} si l'utilisateur n'en a pas
     * @param roles       rôles applicatifs, en majuscules
     */
    public record Profil(String structureId, Set<String> roles) {

        public boolean estResponsableQualite() {
            return roles.contains(RESPONSABLE_QUALITE);
        }

        /**
         * Voit-il l'ensemble des structures ?
         *
         * <p>Le responsable qualité, parce que sa fonction le suppose ; l'administration générale,
         * parce qu'elle administre. Ce test manquait pour {@code SUPER_ADMIN} : seuls les rôles
         * techniques du jeton — {@code ADMIN}, {@code MANAGE} — étaient consultés, si bien qu'un
         * super administrateur dont le jeton ne portait pas l'un d'eux était traité comme un
         * utilisateur ordinaire et ne voyait que sa propre structure.</p>
         */
        public boolean voitToutesLesStructures() {
            return estResponsableQualite()
                    || roles.stream().anyMatch(ROLES_ADMINISTRATION::contains);
        }

        static Profil vide() {
            return new Profil(null, Set.of());
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

            Object roles = reponse.get("roles") != null ? reponse.get("roles") : reponse.get("appRoles");
            Set<String> nomsDeRoles = (roles instanceof List<?> liste)
                    ? liste.stream()
                            .filter(java.util.Objects::nonNull)
                            .map(r -> r.toString().trim().toUpperCase())
                            .filter(r -> !r.isEmpty())
                            .collect(Collectors.toSet())
                    : Set.of();

            return new Profil(structureId, nomsDeRoles);
        } catch (Exception e) {
            log.error("Profil de {} introuvable auprès de user-service : {}", userId, e.getMessage());
            return null;
        }
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