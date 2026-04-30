package com.qualiapproche.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

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
}
