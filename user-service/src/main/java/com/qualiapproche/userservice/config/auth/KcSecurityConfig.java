package com.qualiapproche.userservice.config.auth;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.config.utils.KcJwtRoleConverter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

import java.util.Arrays;

/**
 * Chaîne de sécurité de user-service.
 *
 * <p>Les points d'entrée publics — ouverture et fin de session, liens reçus par courriel — étaient
 * soustraits à la chaîne par {@code web.ignoring()} : ils échappaient alors aussi aux en-têtes de
 * sécurité (anti-cadrage, {@code nosniff}, cache), que les scanners relèvent sur la page même où
 * l'on saisit son mot de passe. Ils passent désormais par la chaîne, en {@code permitAll}.</p>
 *
 * <p>Le jeton n'y est pas lu pour autant ({@link #cookieBearerTokenResolver()}) : un cookie
 * {@code access_token} expiré, présent sur le navigateur, aurait sinon fait répondre 401 à
 * {@code /refresh} — précisément l'appel qui doit le remplacer — et à {@code /login}.</p>
 */
@Configuration
@EnableWebMvc
@EnableWebSecurity
public class KcSecurityConfig {

    /** Points d'entrée ouverts sans session : ils servent à en obtenir une, ou à en sortir. */
    static final String[] CHEMINS_PUBLICS = {
            "/api/v1/login",
            "/api/v1/refresh",
            "/api/v1/logout",
            "/api/v1/verify-email",
            "/api/v1/initiate-reset-pwd",
            "/api/v1/reinitialize-pwd"
    };

    /** Documentation de l'API : publique quand elle est servie, ce que QUALISIRA_SWAGGER décide. */
    static final String[] CHEMINS_DOCUMENTATION = {"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"};

    private static final RequestMatcher SANS_JETON = new OrRequestMatcher(
            Arrays.stream(CHEMINS_PUBLICS).map(AntPathRequestMatcher::antMatcher).toArray(RequestMatcher[]::new));

    @Autowired
    private KcAuthProperties kcAuthProperties;

    /** En production (HTTPS), les cookies doivent être Secure. Injectez via application.yml : cookie.secure=true */
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
            KcAuthenticationEntryPoint entryPoint,
            KcAccessDenied accessDenied) throws Exception {
        DelegatingJwtGrantedAuthoritiesConverter authoritiesConverter = new DelegatingJwtGrantedAuthoritiesConverter(
                new JwtGrantedAuthoritiesConverter(),
                new KcJwtRoleConverter(kcAuthProperties.getClientId()));

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return httpSecurity
                // Aucune page de ce service n'a à s'afficher dans un cadre : l'anti-cadrage était
                // coupé sans raison, et ouvrait la voie au détournement de clic.
                .headers(header -> header.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .anonymous(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    // Seule la santé est publique : le reste de l'actuator décrit l'installation.
                    auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/js/**", "/images/**", "/i18n/**", "/webjars/**").permitAll();
                    auth.requestMatchers(CHEMINS_DOCUMENTATION).permitAll();
                    auth.requestMatchers(CHEMINS_PUBLICS).permitAll();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(eh -> {
                    eh.authenticationEntryPoint(entryPoint);
                    eh.accessDeniedHandler(accessDenied);
                })
                .oauth2ResourceServer(osr -> osr
                        .bearerTokenResolver(cookieBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    /**
     * Décodeur des jetons Keycloak, résolu à la première requête authentifiée.
     *
     * <p>{@code JwtDecoders.fromIssuerLocation} interroge Keycloak dès sa création : un Keycloak
     * pas encore prêt empêchait user-service de démarrer. Différée, la résolution vérifie les mêmes
     * signatures et le même émetteur.</p>
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String emetteur = kcAuthProperties.getIssuerUri();
        return new SupplierJwtDecoder(() -> JwtDecoders.fromIssuerLocation(emetteur));
    }

    @Bean
    GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }

    /**
     * Résolveur de token Bearer qui lit en priorité le cookie {@code access_token}, hors des
     * points d'entrée publics.
     * Si le cookie est absent, il retombe sur l'header {@code Authorization: Bearer ...}
     * standard (compatibilité avec Swagger UI / appels API directs).
     */
    @Bean
    public BearerTokenResolver cookieBearerTokenResolver() {
        return (HttpServletRequest request) -> {
            // 0. Les points d'entrée publics ne lisent aucun jeton (voir la note de classe).
            if (SANS_JETON.matches(request)) {
                return null;
            }
            // 1. Lecture depuis le cookie HTTP-Only
            Optional<String> cookieToken = CookieUtils.getAccessToken(request);
            if (cookieToken.isPresent() && !cookieToken.get().isBlank()) {
                return cookieToken.get();
            }
            // 2. Fallback : header Authorization: Bearer <token> (ex. Swagger)
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return null;
        };
    }
}
