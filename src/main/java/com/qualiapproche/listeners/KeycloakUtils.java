package com.qualiapproche.listeners;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakUtils {
    public static Jwt getCurrentUserToken() {
        return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static String getCurrentUsername() {
        return getCurrentUserToken().getClaim("preferred_username").toString();
    }
    public static String getUserFullname() {
        return getCurrentUserToken().getClaim("name").toString();
    }
    public static String getUserStructure() {
        return "";
    }

    public static String getUserEmail() {
        return getCurrentUserToken().getClaim("email").toString();
    }

    public static String getCurrentUserId() {
        return getCurrentUserToken().getSubject();
    }

  public static String getCurrentUserEmail() {
    return getCurrentUserToken().getClaim("email").toString();
  }
}
