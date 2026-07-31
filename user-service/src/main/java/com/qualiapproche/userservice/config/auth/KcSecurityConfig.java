package com.qualiapproche.userservice.config.auth;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.config.utils.KcJwtRoleConverter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;

@Configuration
@EnableWebMvc
@EnableWebSecurity
public class KcSecurityConfig {

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
                .headers(header -> header.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .anonymous(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/**").permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(
                            "/js/**",
                            "/images/**",
                            "/i18n/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/webjars/**",
                            "/test/**",
                            "/api/v1/login",
                            "/api/v1/refresh",
                            "/api/v1/logout",
                            "/api/v1/verify-email",
                            "/api/v1/initiate-reset-pwd",
                            "/api/v1/reinitialize-pwd").permitAll();
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

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/api/v1/login",
                "/api/v1/refresh",
                "/api/v1/logout",
                "/api/v1/verify-email",
                "/api/v1/initiate-reset-pwd",
                "/api/v1/reinitialize-pwd",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(kcAuthProperties.getIssuerUri());
    }

    @Bean
    GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }

    /**
     * Résolveur de token Bearer qui lit en priorité le cookie {@code access_token}.
     * Si le cookie est absent, il retombe sur l'header {@code Authorization: Bearer ...}
     * standard (compatibilité avec Swagger UI / appels API directs).
     */
    @Bean
    public BearerTokenResolver cookieBearerTokenResolver() {
        return (HttpServletRequest request) -> {
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