package com.qualiapproche.userservice.config.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utilitaire pour la gestion des cookies d'authentification HTTP-Only.
 * Les tokens JWT sont stockés dans des cookies sécurisés afin d'éviter
 * toute exposition aux attaques XSS (contrairement au localStorage).
 */
public final class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE  = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    /** Durée de vie du cookie access_token  (1 heure en secondes). */
    private static final int ACCESS_TOKEN_MAX_AGE  = 3600;

    /** Durée de vie du cookie refresh_token (30 jours en secondes). */
    private static final int REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 30;

    private CookieUtils() {}

    // -------------------------------------------------------------------------
    // Création des cookies
    // -------------------------------------------------------------------------

    /**
     * Ajoute le cookie {@code access_token} HttpOnly à la réponse.
     *
     * @param response     réponse HTTP
     * @param accessToken  valeur du token JWT
     * @param secure       {@code true} en production (HTTPS), {@code false} en dev (HTTP)
     */
    public static void addAccessTokenCookie(HttpServletResponse response,
                                            String accessToken,
                                            boolean secure) {
        ResponseCookie cookie = buildCookie(ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_MAX_AGE, secure);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Ajoute le cookie {@code refresh_token} HttpOnly à la réponse.
     *
     * @param response      réponse HTTP
     * @param refreshToken  valeur du refresh token
     * @param secure        {@code true} en production (HTTPS)
     */
    public static void addRefreshTokenCookie(HttpServletResponse response,
                                             String refreshToken,
                                             boolean secure) {
        ResponseCookie cookie = buildCookie(REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_MAX_AGE, secure);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // -------------------------------------------------------------------------
    // Effacement des cookies (logout)
    // -------------------------------------------------------------------------

    /**
     * Efface le cookie {@code access_token} en le remplaçant par un cookie expiré.
     */
    public static void clearAccessTokenCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = buildCookie(ACCESS_TOKEN_COOKIE, "", 0, secure);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Efface le cookie {@code refresh_token} en le remplaçant par un cookie expiré.
     */
    public static void clearRefreshTokenCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = buildCookie(REFRESH_TOKEN_COOKIE, "", 0, secure);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Efface les deux cookies d'authentification en une seule opération.
     */
    public static void clearAuthCookies(HttpServletResponse response, boolean secure) {
        clearAccessTokenCookie(response, secure);
        clearRefreshTokenCookie(response, secure);
    }

    // -------------------------------------------------------------------------
    // Lecture des cookies
    // -------------------------------------------------------------------------

    /**
     * Extrait la valeur du cookie {@code access_token} depuis la requête.
     *
     * @return valeur du token ou {@link Optional#empty()} si absent
     */
    public static Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * Extrait la valeur du cookie {@code refresh_token} depuis la requête.
     *
     * @return valeur du refresh token ou {@link Optional#empty()} si absent
     */
    public static Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    private static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * Construit un {@link ResponseCookie} sécurisé HttpOnly.
     * <p>
     * {@code SameSite=Lax} convient pour le dev et la production same-domain.
     * Si le frontend est sur un domaine différent, passez à {@code SameSite=None}
     * et forcez {@code secure=true}.
     */
    private static ResponseCookie buildCookie(String name, String value, int maxAge, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
