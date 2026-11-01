package com.qualiapproche.common.cloisonnement;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * La direction pour le compte de laquelle le geste en cours s'exécute : le client, au sens de
 * l'installation partagée.
 *
 * <p>Chaque client est une structure de type {@code DIRECTION} sans parent ; toutes ses données
 * portent son identifiant dans la colonne {@code direction_id}, et Hibernate ({@code @TenantId}
 * sur {@code AuditEntity}) ne rend à une session que les lignes de sa direction. Cette classe dit
 * laquelle :</p>
 * <ol>
 *   <li>celle qu'un traitement a imposée ({@link #pour}) — un message reçu, une tâche planifiée qui
 *       passe d'un client à l'autre, la création d'un client ;</li>
 *   <li>pour un utilisateur, le claim {@code direction_id} de son jeton ; un compte qui n'en porte
 *       pas reçoit {@link #AUCUNE} et ne voit rien — c'est le cas de l'administrateur de la
 *       plateforme, qui crée les clients sans lire leurs données ;</li>
 *   <li>pour un compte de service, l'en-tête {@link #EN_TETE} posé par le service appelant, qui
 *       agit pour le compte d'une direction ; sans en-tête, {@link #INSTALLATION} ;</li>
 *   <li>sans authentification — démarrage, ordonnanceur, consommateur de messages —
 *       {@link #INSTALLATION}.</li>
 * </ol>
 *
 * <p>{@link #INSTALLATION} est la racine : la session voit toutes les directions et peut écrire
 * pour n'importe laquelle, à condition de le dire ; ce qu'elle écrit sans préciser revient à
 * l'installation elle-même et n'est visible d'aucun client.</p>
 */
public final class Cloisonnement {

    /** La racine : le système lui-même, et la direction des données qui n'appartiennent à aucun client. */
    public static final UUID INSTALLATION = new UUID(0L, 0L);

    /** Un compte sans direction : il ne voit rien et n'écrit rien qui soit à un client. */
    public static final UUID AUCUNE = new UUID(0L, 1L);

    /** La direction pour laquelle un service en appelle un autre avec son propre jeton. */
    public static final String EN_TETE = "X-Direction-Id";

    /** Le claim du jeton qui porte la direction de l'utilisateur. */
    public static final String CLAIM = "direction_id";

    private static final String PREFIXE_COMPTE_DE_SERVICE = "service-account-";

    private static final ThreadLocal<UUID> IMPOSEE = new ThreadLocal<>();

    private Cloisonnement() {
    }

    /** La direction du geste en cours ; jamais {@code null}. */
    public static UUID courante() {
        UUID imposee = IMPOSEE.get();
        if (imposee != null) {
            return imposee;
        }
        Jwt jwt = jeton();
        if (jwt == null) {
            return INSTALLATION;
        }
        if (estUnCompteDeService(jwt)) {
            UUID annoncee = uuid(enTeteRecu());
            return annoncee != null ? annoncee : INSTALLATION;
        }
        UUID direction = uuid(jwt.getClaimAsString(CLAIM));
        return direction != null ? direction : AUCUNE;
    }

    /** La direction du client pour lequel on agit, ou {@code null} hors de tout client. */
    public static UUID directionDuClient() {
        UUID courante = courante();
        return estUnClient(courante) ? courante : null;
    }

    /** Vrai pour une vraie direction, faux pour la racine et pour un compte sans direction. */
    public static boolean estUnClient(UUID direction) {
        return direction != null && !INSTALLATION.equals(direction) && !AUCUNE.equals(direction);
    }

    public static boolean estLaRacine(Object direction) {
        return INSTALLATION.equals(direction);
    }

    /**
     * Exécute {@code geste} pour le compte de {@code direction}.
     *
     * <p>Hibernate fixe la direction d'une session à son ouverture : un geste qui doit lire ou
     * écrire sous une autre direction que celle de la requête en cours ouvre donc sa propre
     * transaction ({@code REQUIRES_NEW}) à l'intérieur — voir {@link ExecutionCloisonnee}.</p>
     */
    public static <T> T pour(UUID direction, Supplier<T> geste) {
        UUID precedente = IMPOSEE.get();
        IMPOSEE.set(direction == null ? INSTALLATION : direction);
        try {
            return geste.get();
        } finally {
            if (precedente == null) {
                IMPOSEE.remove();
            } else {
                IMPOSEE.set(precedente);
            }
        }
    }

    /** Variante sans résultat de {@link #pour(UUID, Supplier)}. */
    public static void pour(UUID direction, Runnable geste) {
        pour(direction, () -> {
            geste.run();
            return null;
        });
    }

    /** Lit une direction transmise en texte ; {@code null} si elle est absente ou mal formée. */
    public static UUID uuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean estUnCompteDeService(Jwt jwt) {
        String nom = jwt.getClaimAsString("preferred_username");
        return nom != null && nom.startsWith(PREFIXE_COMPTE_DE_SERVICE);
    }

    private static Jwt jeton() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Jwt jwt ? jwt : null;
    }

    private static String enTeteRecu() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader(EN_TETE);
        }
        return null;
    }
}
