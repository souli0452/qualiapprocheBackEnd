package com.qualiapproche.userservice.config;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Client HTTP vers Keycloak : jetons, renouvellement, révocation.
 *
 * <p>Il faisait confiance à <b>n'importe quel</b> certificat ({@code InsecureTrustManagerFactory}) :
 * entre user-service et Keycloak passent les mots de passe des utilisateurs et le secret du client,
 * et un intermédiaire muni d'un certificat quelconque pouvait tout lire. Le magasin de confiance par
 * défaut de la JVM s'applique désormais. Un Keycloak servi par un certificat d'autorité interne se
 * déclare en ajoutant cette autorité au magasin de la JVM (ou par
 * {@code -Djavax.net.ssl.trustStore}), pas en désactivant la vérification.</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final KcAuthProperties kcAuthProperties;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(kcAuthProperties.getIssuerUri())
                .filter(errorHandler())
                .build();
    }

    /**
     * Une réponse d'erreur de Keycloak devient une {@link ReponseKeycloakEnErreur} qui garde son
     * statut : c'est lui qui distingue des identifiants refusés (4xx), à dire 401 à l'utilisateur,
     * d'un Keycloak en panne (5xx), qui reste une erreur du serveur.
     */
    private static ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError() || clientResponse.statusCode().is4xxClientError()) {
                int statut = clientResponse.statusCode().value();
                return clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(errorBody -> Mono.error(new ReponseKeycloakEnErreur(statut, errorBody)));
            } else {
                return Mono.just(clientResponse);
            }
        });
    }

    /** Keycloak a répondu par une erreur ; le statut est conservé pour que l'appelant en décide. */
    public static class ReponseKeycloakEnErreur extends RuntimeException {
        private final int statut;

        public ReponseKeycloakEnErreur(int statut, String corps) {
            super(corps);
            this.statut = statut;
        }

        public int getStatut() {
            return statut;
        }

        /** Keycloak refuse ce qu'on lui présente — identifiants, jeton — plutôt qu'il ne tombe en panne. */
        public boolean refusDeLAppelant() {
            return statut >= 400 && statut < 500;
        }
    }
}
