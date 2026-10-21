package com.qualiapproche.userservice.controller;

import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.userservice.service.KcUserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comptes et mots de passe, vus de l'extérieur : sous la vraie chaîne de sécurité, avec des jetons
 * simulés.
 *
 * <p>Trois choses y sont tenues. Qui peut appeler quoi — 401 sans jeton, 403 sans la permission,
 * 200 avec. Que les secrets voyagent dans le <b>corps</b> et nulle part ailleurs — un mot de passe
 * passé dans l'URL n'est plus lu. Et que {@code /update-pwd} ne serve plus d'oracle : on n'y
 * éprouve que son propre mot de passe.</p>
 */
@SpringJUnitWebConfig(ContexteDeSecuriteDeTest.class)
class KcUserControllerSecuriteTest {

    private static final String MOI = "11111111-1111-4111-8111-111111111111";

    @Autowired
    private WebApplicationContext contexte;
    @Autowired
    private KcUserService service;

    private MockMvc mvc;

    @BeforeEach
    void preparer() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
    }

    /** Un utilisateur connecté, sans autre autorité que celles données. */
    private static JwtRequestPostProcessor connecte(String nom, String... autorites) {
        return jwt().jwt(j -> j.subject(MOI).claim("preferred_username", nom))
                .authorities(java.util.Arrays.stream(autorites).<org.springframework.security.core.GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
    }

    private static JwtRequestPostProcessor gestionnaire() {
        return connecte("gestionnaire", "ROLE_MANAGE_USER");
    }

    @Nested
    @DisplayName("Accès aux points d'entrée")
    class Acces {

        @Test
        @DisplayName("Sans jeton, le profil est refusé en 401")
        void sansJeton_401() throws Exception {
            mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("L'annuaire est lisible par tout compte connecté")
        void annuaire_ouvert() throws Exception {
            mvc.perform(get("/api/v1/users").with(connecte("agent"))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Créer un compte : 401 sans jeton, 403 sans MANAGE_USER, 200 avec")
        void creation() throws Exception {
            String corps = "{\"username\":\"b.kabore\",\"email\":\"b@exemple.bf\"}";
            when(service.createUser(any())).thenReturn(new KcUserDto());

            mvc.perform(post("/api/v1/users/create").contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/users/create").with(connecte("agent"))
                            .contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/users/create").with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Un compte de service crée un compte sans permission applicative")
        void creation_parUnService() throws Exception {
            when(service.createUser(any())).thenReturn(new KcUserDto());

            mvc.perform(post("/api/v1/users/create").with(connecte("service-account-quali-sira"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Changer le statut d'un compte exige MANAGE_USER")
        void statut() throws Exception {
            mvc.perform(put("/api/v1/users/change-status").param("userId", "u").param("enabled", "false")
                            .with(connecte("agent")))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/users/change-status").param("userId", "u").param("enabled", "false")
                            .with(gestionnaire()))
                    .andExpect(status().isOk());
            verify(service).changeUserStatus("u", false);
        }

        @Test
        @DisplayName("De l'actuator, seule la santé échappe à l'authentification")
        void actuator() throws Exception {
            mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
            mvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
            // Pas d'actuator dans ce contexte : 404, mais surtout pas 401.
            mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Points d'entrée publics")
    class Publics {

        @Test
        @DisplayName("Un cookie de session expiré n'empêche ni la connexion ni le renouvellement")
        void cookiePerime_ignore() throws Exception {
            when(service.login(any(), any())).thenReturn(Map.of());
            when(service.refreshToken(any(), any())).thenReturn(ResponseEntity.ok().build());
            Cookie perime = new Cookie("access_token", "jeton.perime.illisible");

            mvc.perform(post("/api/v1/login").cookie(perime).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"a\",\"password\":\"b\"}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/v1/refresh").cookie(perime)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Les en-têtes de sécurité s'appliquent aussi aux points publics : anti-cadrage DENY")
        void enTetes() throws Exception {
            when(service.login(any(), any())).thenReturn(Map.of());

            mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"a\",\"password\":\"b\"}"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("Demande de lien : l'adresse dans le corps, sans être connecté")
        void demandeDeLien() throws Exception {
            mvc.perform(post("/api/v1/initiate-reset-pwd").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a.traore@exemple.bf\"}"))
                    .andExpect(status().isOk());
            verify(service).initiatePasswordReset("a.traore@exemple.bf");
        }

        @Test
        @DisplayName("Demande de lien : une adresse mal formée, ou passée dans l'URL, est refusée en 400")
        void demandeDeLien_malFormee() throws Exception {
            mvc.perform(post("/api/v1/initiate-reset-pwd").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"pas-une-adresse\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/api/v1/initiate-reset-pwd").param("email", "a.traore@exemple.bf"))
                    .andExpect(status().is4xxClientError());
            verify(service, never()).initiatePasswordReset(anyString());
        }

        @Test
        @DisplayName("Réinitialisation par lien : jeton et mot de passe lus dans le corps")
        void reinitialisationParLien() throws Exception {
            mvc.perform(put("/api/v1/reinitialize-pwd").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"u-1\",\"token\":\"jeton\",\"password\":\"Nouveau-1\"}"))
                    .andExpect(status().isOk());
            verify(service).reinitializePwd("u-1", "Nouveau-1", "jeton");
        }

        @Test
        @DisplayName("Réinitialisation par lien : l'ancienne forme, secrets dans l'URL, n'est plus lue")
        void reinitialisationParLien_urlRefusee() throws Exception {
            mvc.perform(put("/api/v1/reinitialize-pwd")
                            .param("userId", "u-1").param("token", "jeton").param("password", "Nouveau-1"))
                    .andExpect(status().is4xxClientError());
            mvc.perform(put("/api/v1/reinitialize-pwd").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"u-1\",\"password\":\"Nouveau-1\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("token")));
            verify(service, never()).reinitializePwd(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Mot de passe fixé par un gestionnaire")
    class ParGestionnaire {

        private static final String CORPS = "{\"userId\":\"u-2\",\"password\":\"Provisoire-1\"}";

        @Test
        @DisplayName("401 sans jeton, 403 sans MANAGE_USER, 200 avec")
        void acces() throws Exception {
            mvc.perform(patch("/api/v1/users/reset-password").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                    .andExpect(status().isUnauthorized());
            mvc.perform(patch("/api/v1/users/reset-password").with(connecte("agent"))
                            .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/users/reset-password").with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                    .andExpect(status().isOk());
            verify(service).fixerMotDePasseParGestionnaire("u-2", "Provisoire-1");
        }

        @Test
        @DisplayName("Le mot de passe dans l'URL n'est plus lu ; vide dans le corps, il est refusé")
        void corpsExige() throws Exception {
            mvc.perform(patch("/api/v1/users/reset-password").with(gestionnaire())
                            .param("userId", "u-2").param("password", "Provisoire-1"))
                    .andExpect(status().is4xxClientError());
            mvc.perform(patch("/api/v1/users/reset-password").with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"u-2\",\"password\":\" \"}"))
                    .andExpect(status().isBadRequest());
            verify(service, never()).fixerMotDePasseParGestionnaire(anyString(), anyString());
        }

        @Test
        @DisplayName("Un mot de passe démesuré est refusé avant d'atteindre Keycloak")
        void tailleBornee() throws Exception {
            String long129 = "x".repeat(129);
            mvc.perform(patch("/api/v1/users/reset-password").with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":\"u-2\",\"password\":\"" + long129 + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Changement de son mot de passe (/update-pwd)")
    class SonMotDePasse {

        private String corps(String username) {
            return "{\"username\":\"" + username + "\",\"oldPassword\":\"Ancien-1\",\"password\":\"Nouveau-1\"}";
        }

        @Test
        @DisplayName("Sur son propre compte : accepté, nom comparé sans égard à la casse")
        void sonCompte() throws Exception {
            when(service.updatePassword(anyString(), anyString(), anyString())).thenReturn(ResponseEntity.ok().build());

            mvc.perform(put("/api/v1/update-pwd").with(connecte("a.traore"))
                            .contentType(MediaType.APPLICATION_JSON).content(corps("A.Traore")))
                    .andExpect(status().isOk());
            verify(service).updatePassword("A.Traore", "Ancien-1", "Nouveau-1");
        }

        @Test
        @DisplayName("Sur le compte d'un autre : 403, et Keycloak n'est même pas interrogé")
        void compteDAutrui_refuse() throws Exception {
            mvc.perform(put("/api/v1/update-pwd").with(connecte("a.traore"))
                            .contentType(MediaType.APPLICATION_JSON).content(corps("admin")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("votre propre compte")));
            verify(service, never()).updatePassword(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Un gestionnaire des utilisateurs peut viser un autre compte")
        void gestionnaire_autreCompte() throws Exception {
            when(service.updatePassword(anyString(), anyString(), anyString())).thenReturn(ResponseEntity.ok().build());

            mvc.perform(put("/api/v1/update-pwd").with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON).content(corps("a.traore")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Sans jeton : 401 ; sans ancien mot de passe : 400")
        void garde() throws Exception {
            mvc.perform(put("/api/v1/update-pwd").contentType(MediaType.APPLICATION_JSON).content(corps("a.traore")))
                    .andExpect(status().isUnauthorized());
            mvc.perform(put("/api/v1/update-pwd").with(connecte("a.traore")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"a.traore\",\"password\":\"Nouveau-1\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/v1/update-pwd").with(connecte("a.traore")).contentType(MediaType.APPLICATION_JSON)
                            .content("{pas du json"))
                    .andExpect(status().isBadRequest());
            verify(service, never()).updatePassword(anyString(), anyString(), anyString());
        }
    }
}
