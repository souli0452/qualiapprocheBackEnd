package com.qualiapproche.evaluation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Les appels d'evaluation-service vers les autres modules.
 *
 * <p><b>Toujours sous le jeton de l'utilisateur</b>, jamais sous celui du service : ce service n'a
 * pas de compte client Keycloak, et lui en donner un demanderait un secret sur le serveur. C'est
 * aussi ce qui convient — un écart transmis aux non-conformités est déclaré par l'auditeur qui le
 * publie, et il doit en être l'auteur. Un appel sans requête utilisateur (tâche de nuit) part donc
 * sans jeton, et est refusé : la reprise se fait depuis l'audit, par un geste de l'utilisateur.</p>
 *
 * <p>L'en-tête {@code X-User-Permissions}, que la gateway établit, est propagé avec : le service
 * appelé en tire les permissions applicatives que le jeton ne porte pas. La gateway écrase tout
 * en-tête de ce nom venu du client — ce qui circule ici est ce qu'elle a établi.</p>
 */
@Configuration
public class FeignConfig {

    private static final String PERMISSIONS = "X-User-Permissions";

    @Bean
    public RequestInterceptor relaisDeLUtilisateur() {
        return gabarit -> {
            Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
            if (authentification instanceof JwtAuthenticationToken jeton) {
                gabarit.header("Authorization", "Bearer " + jeton.getToken().getTokenValue());
            }
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet) {
                String permissions = servlet.getRequest().getHeader(PERMISSIONS);
                if (permissions != null && !permissions.isBlank()) {
                    gabarit.header(PERMISSIONS, permissions);
                    // La signature de la passerelle voyage avec : sans elle, le service appelé ignore l'en-tête.
                    String signature = servlet.getRequest().getHeader("X-User-Permissions-Signature");
                    if (signature != null) {
                        gabarit.header("X-User-Permissions-Signature", signature);
                    }
                }
            }
        };
    }

    /** Défait l'enveloppe {@code ApiResponse} que GlobalResponseHandler pose sur toute réponse. */
    @Bean
    public Decoder decodeurDEnveloppe(ObjectMapper objectMapper) {
        return new ApiResponseFeignDecoder(objectMapper);
    }
}
