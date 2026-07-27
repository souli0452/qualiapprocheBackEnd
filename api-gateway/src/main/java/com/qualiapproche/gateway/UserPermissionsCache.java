package com.qualiapproche.gateway;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;

/**
 * Cache court des permissions applicatives résolues via user-service (voir {@link AuthCookieFilter}).
 * Évite d'appeler user-service à chaque requête vers support-service : le résultat d'un
 * access_token donné est réutilisé pendant {@code gateway.permissions-cache-ttl-seconds}
 * (60s par défaut) avant d'être re-résolu.
 *
 * <p>Clé = hash SHA-256 du token (pas le token en clair, pour ne pas le garder inutilement
 * en mémoire au-delà de la requête qui l'a reçu).
 */
@Component
public class UserPermissionsCache {

    private final Cache<String, String[]> cache;

    public UserPermissionsCache(@Value("${gateway.permissions-cache-ttl-seconds:60}") long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(10_000)
                .build();
    }

    public Optional<String[]> get(String token) {
        return Optional.ofNullable(cache.getIfPresent(hash(token)));
    }

    public void put(String token, String[] permissions) {
        cache.put(hash(token), permissions);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est toujours disponible sur une JVM standard ; filet de sécurité improbable.
            return token;
        }
    }
}
