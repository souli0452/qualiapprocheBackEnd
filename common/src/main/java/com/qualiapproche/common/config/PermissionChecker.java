package com.qualiapproche.common.config;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.utils.SecurityUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class PermissionChecker {

    private static final String PERMISSIONS_HEADER = "X-User-Permissions";

    public boolean canCreate(final Object controller) {
        return check(controller, RequirePermissions::create);
    }

    public boolean canUpdate(final Object controller) {
        return check(controller, RequirePermissions::update);
    }

    public boolean canRead(final Object controller) {
        return check(controller, RequirePermissions::read);
    }

    public boolean canDelete(final Object controller) {
        return check(controller, RequirePermissions::delete);
    }

    public boolean canValidate(final Object controller) {
        return check(controller, RequirePermissions::validate);
    }

    public boolean canCorrected(final Object controller) {
        return check(controller, RequirePermissions::corrected);
    }

    public boolean canResendActivation(final Object controller) {
        return check(controller, RequirePermissions::resendActivation);
    }

    public boolean canResetPassword(final Object controller) {
        return check(controller, RequirePermissions::resetPassword);
    }

    /**
     * L'appelant détient-il l'un de ces rôles ou permissions ?
     *
     * <p>Contrairement aux {@code canXxx}, ne consulte pas l'annotation du contrôleur : sert à
     * décider d'une <b>portée</b> — voir les dossiers de toutes les structures plutôt que de la
     * sienne — et non d'un droit d'accès au point d'entrée, lequel reste porté par
     * {@link RequirePermissions}.</p>
     *
     * <p>Même source et mêmes équivalences que le reste : rôles du jeton et permissions
     * applicatives, avec ou sans préfixe {@code ROLE_}.</p>
     */
    public boolean detient(final String... rolesOuPermissions) {
        if (rolesOuPermissions == null || rolesOuPermissions.length == 0) {
            return false;
        }
        Set<String> userPerms = resolveUserPermissions();
        if (userPerms.isEmpty()) {
            return false;
        }
        return Arrays.stream(rolesOuPermissions)
                .filter(java.util.Objects::nonNull)
                .map(String::toUpperCase)
                .anyMatch(req -> userPerms.contains(req)
                        || userPerms.contains("ROLE_" + req)
                        || (req.startsWith("ROLE_") && userPerms.contains(req.substring(5))));
    }

    /**
     * L'appel vient-il d'un <b>service</b>, et non d'un utilisateur ?
     *
     * <p>Certains points d'entrée n'existent que pour le dialogue entre services : les callbacks
     * d'avancement de circuit, la déclaration d'un fait, la redésignation d'un titulaire. Laissés
     * sous la seule authentification, n'importe quel agent muni d'un jeton valide pouvait y poster —
     * faire passer un document à « validé » sans décision de circuit, ou déclarer « plans soldés »
     * et déverrouiller une clôture.</p>
     *
     * <p>Deux preuves sont admises, l'une ou l'autre :</p>
     * <ul>
     *   <li>le jeton est celui d'un <b>compte de service</b> Keycloak : {@code preferred_username}
     *       commence par {@code service-account-}. Ce nom est posé par Keycloak lui-même sur les
     *       jetons {@code client_credentials} — l'obtenir exige le secret du client, un utilisateur
     *       ne peut pas se le donner ;</li>
     *   <li>le porteur détient le rôle {@code SERVICE_TECHNIQUE}, pour les déploiements qui
     *       préfèrent un rôle explicite accordé au compte de service.</li>
     * </ul>
     */
    public boolean appelDeService() {
        Jwt jwt = SecurityUtils.getJwt();
        String nom = jwt != null ? jwt.getClaimAsString("preferred_username") : null;
        if (nom != null && nom.startsWith("service-account-")) {
            return true;
        }
        return detient("SERVICE_TECHNIQUE");
    }

    private boolean check(final Object controller, final Function<RequirePermissions, String[]> extractor) {
        RequirePermissions ann = controller.getClass().getAnnotation(RequirePermissions.class);
        if (ann == null) {
            return false;
        }

        String[] required = extractor.apply(ann);
        if (required.length == 0) {
            return true;
        }

        Set<String> userPerms = resolveUserPermissions();
        if (userPerms.isEmpty()) {
            return false;
        }

        return Arrays.stream(required)
                .map(String::toUpperCase)
                .anyMatch(req -> userPerms.contains(req)
                        || userPerms.contains("ROLE_" + req)
                        || (req.startsWith("ROLE_") && userPerms.contains(req.substring(5))));
    }

    /**
     * Combine deux sources de permissions :
     * 1. Les rôles Keycloak du JWT ({@code realm_access.roles}, convertis en GrantedAuthority) ;
     * 2. Les permissions applicatives (AppRole, base de user-service) que la gateway résout une
     *    fois par requête et transmet via l'en-tête interne {@code X-User-Permissions}
     *    (voir {@code AuthCookieFilter} côté api-gateway). Un appel direct à un service (sans
     *    passer par la gateway) n'aura donc que la source 1.
     */
    private Set<String> resolveUserPermissions() {
        Set<String> perms = new HashSet<>();

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(String::toUpperCase)
                    .forEach(perms::add);
        }

        String header = currentRequestHeader(PERMISSIONS_HEADER);
        if (header != null && !header.isBlank()) {
            Arrays.stream(header.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .forEach(perms::add);
        }

        return perms;
    }

    private String currentRequestHeader(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest().getHeader(name);
        }
        return null;
    }
}
