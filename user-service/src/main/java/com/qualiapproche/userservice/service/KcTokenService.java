package com.qualiapproche.userservice.service;

import com.qualiapproche.userservice.config.auth.KcConstants;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.common.dto.auth.KcLoginRequestDto;
import com.qualiapproche.common.dto.auth.KcTokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

/**
 * Dialogue avec le point de jetons de Keycloak : ouverture de session, renouvellement, révocation.
 *
 * <p>Ce service émettait aussi ses propres jetons ({@code generateToken}/{@code validateToken}),
 * signés HS256 avec une clé écrite en dur dans le code — donc publique. Aucun appelant ne s'en
 * servait plus, mais la clé suffisait à forger un jeton que ce code aurait accepté : la méthode et
 * la dépendance {@code jjwt} ont été retirées. Les seuls jetons que le produit reconnaît sont ceux
 * de Keycloak, vérifiés par sa clé publique.</p>
 */
@Service
@RequiredArgsConstructor
public class KcTokenService {
    private final KcAuthProperties kcAuthProperties;
    private final WebClient webClient;

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
