package com.qualiapproche.workflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ce que le service laisse lire sans jeton : la sonde de santé, et rien d'autre.
 *
 * <p>La chaîne ouvrait {@code /actuator/**} et la documentation OpenAPI : tout point de
 * l'actuator exposé par mégarde ({@code env}, {@code heapdump}…) l'aurait été sans
 * authentification, et la documentation décrivait chaque point d'entrée à qui la demandait. La
 * chaîne réelle est montée ici, avec un décodeur de jetons factice : seule l'autorisation est
 * jugée — 404 veut dire « passé la sécurité, rien à servir », 401 « refusé avant ».</p>
 */
@SpringJUnitWebConfig(SurfacePubliqueTest.ConfigurationDeTest.class)
class SurfacePubliqueTest {

    @Configuration
    @EnableWebMvc
    @Import(WorkflowSecurityConfig.class)
    static class ConfigurationDeTest {
        @Bean JwtDecoder jwtDecoder() { return Mockito.mock(JwtDecoder.class); }
        @Bean DataSource dataSource() { return Mockito.mock(DataSource.class); }
        @Bean ConnectionFactory connectionFactory() { return Mockito.mock(ConnectionFactory.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired private WebApplicationContext contexte;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte)
                .addFilters(contexte.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @Test
    @DisplayName("La sonde de santé reste lisible sans jeton : Eureka et la supervision l'interrogent ainsi")
    void sante_publique() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Le reste de l'actuator exige un jeton")
    void actuator_ferme() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/heapdump")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La documentation OpenAPI se lit sans jeton, la passerelle l'agrège ainsi ; QUALISIRA_SWAGGER décide si elle existe")
    void documentation_sans_jeton() throws Exception {
        // 404 et non 401 : le filtre laisse passer, et rien ne la sert dans ce contexte de test.
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }
}
