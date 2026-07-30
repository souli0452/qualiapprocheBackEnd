package com.qualiapproche.workflow.config;

import com.qualiapproche.common.config.GlobalExceptionHandler;
import com.qualiapproche.common.config.KeycloakRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité du service de workflow.
 *
 * <p>Sans cette configuration, le service retombait sur la chaîne par défaut de Spring Boot :
 * les requêtes étaient bien authentifiées, mais les autorités provenaient du claim
 * {@code scope} et non de {@code realm_access.roles}. {@code SecurityUtils.hasRole(...)}
 * renvoyait donc systématiquement faux, et <b>toute</b> transition portant une habilitation
 * était refusée. {@link KeycloakRoleConverter} rétablit la conversion des rôles Keycloak,
 * à l'identique des autres services métier.</p>
 *
 * <p>Volontairement, seuls {@link KeycloakRoleConverter} et {@link GlobalExceptionHandler}
 * sont importés depuis {@code common.config} plutôt que d'en scanner tout le paquet : celui-ci
 * contient aussi {@code GlobalResponseHandler}, qui encapsulerait les réponses dans
 * {@code ApiResponse} et changerait le format de sortie déjà consommé par les clients Feign
 * et par le front.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import({KeycloakRoleConverter.class, GlobalExceptionHandler.class})
public class WorkflowSecurityConfig {

    private final KeycloakRoleConverter keycloakRoleConverter;

    public WorkflowSecurityConfig(KeycloakRoleConverter keycloakRoleConverter) {
        this.keycloakRoleConverter = keycloakRoleConverter;
    }

    @Bean
    public SecurityFilterChain workflowSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoleConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
