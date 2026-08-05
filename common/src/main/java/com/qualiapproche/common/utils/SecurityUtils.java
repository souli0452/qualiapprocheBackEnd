package com.qualiapproche.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public class SecurityUtils {

    public static Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            return (Jwt) authentication.getPrincipal();
        }
        return null;
    }

    public static String getCurrentUserId() {
        Jwt jwt = getJwt();
        return (jwt != null) ? jwt.getSubject() : null;
    }

    public static String getCurrentUserFullName() {
        Jwt jwt = getJwt();
        if (jwt != null) {
            String name = jwt.getClaimAsString("name");
            return (name != null) ? name : jwt.getClaimAsString("preferred_username");
        }
        return "Système";
    }

    public static String getCurrentUserEmail() {
        Jwt jwt = getJwt();
        return (jwt != null) ? jwt.getClaimAsString("email") : null;
    }

    public static UUID getCurrentDirectionId() {
        Jwt jwt = getJwt();
        if (jwt != null) {
            String directionId = jwt.getClaimAsString("direction_id");
            if (directionId == null) {
                // Fallback to structure_id if direction_id is not present
                directionId = jwt.getClaimAsString("structure_id");
            }
            try {
                return (directionId != null) ? UUID.fromString(directionId) : null;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Structure de rattachement de l'appelant, telle que le jeton la déclare.
     *
     * <p>Sert à borner ce qu'un utilisateur voit : sans elle, les listes générales rendaient les
     * dossiers de toutes les structures à quiconque savait les demander — la restriction n'existait
     * que dans le choix des écrans, pas dans le service.</p>
     *
     * @return l'identifiant de structure, ou {@code null} si le jeton ne le porte pas
     */
    public static String getCurrentUserStructureId() {
        Jwt jwt = getJwt();
        if (jwt == null) {
            return null;
        }
        String structureId = jwt.getClaimAsString("structure_id");
        return structureId == null || structureId.isBlank() ? null : structureId;
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_" + role)
                                    || authority.getAuthority().equalsIgnoreCase(role));
    }
}
