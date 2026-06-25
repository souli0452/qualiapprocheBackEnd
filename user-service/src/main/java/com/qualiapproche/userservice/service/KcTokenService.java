package com.qualiapproche.userservice.service;

import com.qualiapproche.userservice.config.auth.KcConstants;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcTokenDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

@Service
@RequiredArgsConstructor
public class KcTokenService {
    private final KcAuthProperties kcAuthProperties;
    private final WebClient webClient;

    private static final long EXPIRATION_TIME = 3600_000;
    private static final String SECRET_KEY = "r0v6hbs9d5gDp3U7g+YbLs56aZb9qaX2wFxYmzaqFno=";

    public String generateToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public String validateToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public KcTokenDto getAccessToken(KcLoginRequestDto loginRequest) {
        return webClient.post()
                .uri(kcAuthProperties.getTokenUri())
                .body(fromFormData(KcConstants.CLIENT_ID, kcAuthProperties.getClientId())
                        .with(KcConstants.GRANT_TYPE, "password")
                        .with(KcConstants.CLIENT_SECRET, kcAuthProperties.getClientSecret())
                        .with(KcConstants.USERNAME, loginRequest.getUsername())
                        .with(KcConstants.PASSWORD, loginRequest.getPassword()))
                .retrieve()
                .bodyToMono(KcTokenDto.class)
                .block();
    }

    public KcTokenDto getRefreshToken(String refreshToken) {
        return webClient.post()
                .uri(kcAuthProperties.getTokenUri())
                .body(fromFormData(KcConstants.CLIENT_ID, kcAuthProperties.getClientId())
                        .with(KcConstants.GRANT_TYPE, "refresh_token")
                        .with(KcConstants.CLIENT_SECRET, kcAuthProperties.getClientSecret())
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(KcTokenDto.class)
                .block();
    }

    /**
     * Révoque un refresh token auprès de l'endpoint Keycloak {@code /protocol/openid-connect/revoke}.
     * Cela invalide la session côté serveur Keycloak (logout server-side).
     *
     * @param refreshToken le refresh token à révoquer
     */
    public void revokeToken(String refreshToken) {
        String revokeUri = kcAuthProperties.getTokenUri().replace("/token", "/revoke");
        webClient.post()
                .uri(revokeUri)
                .body(fromFormData(KcConstants.CLIENT_ID, kcAuthProperties.getClientId())
                        .with(KcConstants.CLIENT_SECRET, kcAuthProperties.getClientSecret())
                        .with("token", refreshToken)
                        .with("token_type_hint", "refresh_token"))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
