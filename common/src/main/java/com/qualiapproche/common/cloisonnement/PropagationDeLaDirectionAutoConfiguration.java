package com.qualiapproche.common.cloisonnement;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

/**
 * Les appels entre services annoncent la direction pour laquelle ils sont faits.
 *
 * <p>Avec le jeton de l'utilisateur, l'en-tête est redondant (le service appelé lit le claim) ;
 * il compte quand l'appel part avec le jeton du service — chemins techniques, tâches planifiées
 * pour un client. Le service appelé ne le croit que d'un compte de service : un client qui le
 * forgerait n'en tirerait rien.</p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class PropagationDeLaDirectionAutoConfiguration {

    @Bean
    public feign.RequestInterceptor directionAnnoncee() {
        return modele -> {
            UUID direction = Cloisonnement.directionDuClient();
            modele.removeHeader(Cloisonnement.EN_TETE);
            if (direction != null) {
                modele.header(Cloisonnement.EN_TETE, direction.toString());
            }
        };
    }
}
