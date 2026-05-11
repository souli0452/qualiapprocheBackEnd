package com.qualiapproche.userservice.config.auth;

import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.config.utils.KcJwtRoleConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebMvc
@EnableWebSecurity
public class KcSecurityConfig {

    @Autowired
    private KcAuthProperties kcAuthProperties;

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
                    auth.requestMatchers(HttpMethod.OPTIONS, "/*").permitAll();
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
                            "/api/v1/verify-email",
                            "/api/v1/initiate-reset-pwd",
                            "/api/v1/reinitialize-pwd"
                    ).permitAll();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(eh -> {
                    eh.authenticationEntryPoint(entryPoint);
                    eh.accessDeniedHandler(accessDenied);
                })
                .oauth2ResourceServer(osr -> osr.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(kcAuthProperties.getIssuerUri());
    }

    @Bean
    GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(List.of("https://qualisira.horeb.tech"));
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
//        configuration.setAllowCredentials(true);
//        configuration.setAllowedHeaders(List.of("*"));
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/api/v1/**", configuration);
//        return source;
//    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://qualisira.horeb.tech", "http://localhost:4200"));
        // Ajout de OPTIONS et PATCH pour être tranquille
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Utilise /** pour couvrir /user-service, /amelioration-service, etc.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}