package com.qualiapproche.userservice.controller;

import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.UserRoleAssignment;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rôles Keycloak et rôles applicatifs, vus de l'extérieur.
 *
 * <p>Au-delà de « qui peut appeler » (401, 403, 200), ces tests suivent par HTTP les tentatives
 * d'élévation de privilèges que les permissions seules laissaient passer : un gestionnaire des
 * rôles qui s'attribue un rôle, qui en attribue un plus fort que lui, ou qui ajoute au sien une
 * permission qu'il n'a pas — ou qui passe par la porte Keycloak : un rôle du realm nommé comme une
 * habilitation d'étape ({@code RESPONSABLE_QUALITE}) donnait le droit de décider dans les circuits.</p>
 */
@SpringJUnitWebConfig(ContexteDeSecuriteDeTest.class)
class RolesControllersSecuriteTest {

    private static final String MOI = "11111111-1111-4111-8111-111111111111";
    private static final String AUTRE = "22222222-2222-4222-8222-222222222222";

    @Autowired
    private WebApplicationContext contexte;
    @Autowired
    private Keycloak keycloak;
    @Autowired
    private AppRoleRepository roles;
    @Autowired
    private UserRoleAssignmentRepository affectations;

    private MockMvc mvc;

    /** Mon rôle : gérer les rôles, et lire les non-conformités. */
    private final AppRole gestionnaireDesRoles = role("GESTIONNAIRE_ROLES", "ROLE_MANAGE", "nc-read");
    private final AppRole lecteur = role("LECTEUR", "nc-read");
    private final AppRole superAdmin = role("SUPER_ADMIN", "ROLE_MANAGE", "nc-read", "licence-write");
    private final AppRole licence = role("LICENCE", "licence-write");

    @BeforeEach
    void preparer() {
        reset(keycloak, roles, affectations);
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
        porte(MOI, gestionnaireDesRoles);
        porte(AUTRE);
        when(affectations.save(any(UserRoleAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(roles.save(any(AppRole.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static AppRole role(String nom, String... permissions) {
        AppRole role = new AppRole();
        role.setId(UUID.randomUUID());
        role.setName(nom);
        role.setPermissions(new ArrayList<>(List.of(permissions)));
        return role;
    }

    private void porte(String userId, AppRole... sesRoles) {
        when(affectations.findByUserId(userId)).thenReturn(java.util.Arrays.stream(sesRoles)
                .<UserRoleAssignment>map(r -> UserRoleAssignment.builder().userId(userId).role(r).build()).toList());
    }

    private static JwtRequestPostProcessor connecte(String... autorites) {
        return jwt().jwt(j -> j.subject(MOI).claim("preferred_username", "moi"))
                .authorities(java.util.Arrays.stream(autorites).<org.springframework.security.core.GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
    }

    private static JwtRequestPostProcessor gestionnaire() {
        return connecte("ROLE_ROLE_MANAGE");
    }

    @Nested
    @DisplayName("Rôles Keycloak (/api/v1/roles)")
    class RolesKeycloak {

        private RolesResource rolesDuRealm() {
            return keycloak.realm("quali").roles();
        }

        private RoleScopeResource rolesDe(String userId) {
            return keycloak.realm("quali").users().get(userId).roles().realmLevel();
        }

        private void existe(String nom) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(nom);
            when(rolesDuRealm().get(nom).toRepresentation()).thenReturn(role);
        }

        @Test
        @DisplayName("Créer un rôle : 401 sans jeton, 403 sans ROLE_MANAGE, rien n'est créé")
        void creation_acces() throws Exception {
            String corps = "{\"name\":\"AUDITEUR\"}";

            mvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/roles").with(connecte()).contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isForbidden());
            verify(rolesDuRealm(), never()).create(any());
        }

        @Test
        @DisplayName("Un gestionnaire des rôles non SUPER_ADMIN ne crée pas RESPONSABLE_QUALITE côté Keycloak : 403")
        void creation_parUnGestionnaire_refusee() throws Exception {
            mvc.perform(post("/api/v1/roles").with(gestionnaire()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"RESPONSABLE_QUALITE\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("SUPER_ADMIN")));
            verify(rolesDuRealm(), never()).create(any());
        }

        @Test
        @DisplayName("Un gestionnaire des rôles non SUPER_ADMIN n'attribue pas RESPONSABLE_QUALITE à un collègue : 403")
        void attribution_parUnGestionnaire_refusee() throws Exception {
            existe("RESPONSABLE_QUALITE");
            when(rolesDe(AUTRE).listAll()).thenReturn(List.of());

            mvc.perform(post("/api/v1/roles/assign-roles").param("userId", AUTRE).with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON).content("[\"RESPONSABLE_QUALITE\"]"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("SUPER_ADMIN")));
            verify(rolesDe(AUTRE), never()).add(anyList());
            verify(rolesDe(AUTRE), never()).remove(anyList());
        }

        @Test
        @DisplayName("Modifier ou supprimer un rôle Keycloak : 403 pour un gestionnaire non SUPER_ADMIN")
        void modificationEtSuppression_refusees() throws Exception {
            mvc.perform(put("/api/v1/roles/update").with(gestionnaire()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"RESPONSABLE_QUALITE\",\"description\":\"x\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/v1/roles/RESPONSABLE_QUALITE").with(gestionnaire()))
                    .andExpect(status().isForbidden());
            verify(rolesDuRealm(), never()).deleteRole(any());
        }

        @Test
        @DisplayName("Attribuer des rôles : 403 sans ROLE_MANAGE")
        void attribution_sansPermission() throws Exception {
            mvc.perform(post("/api/v1/roles/assign-roles").param("userId", AUTRE).with(connecte())
                            .contentType(MediaType.APPLICATION_JSON).content("[\"AUDITEUR\"]"))
                    .andExpect(status().isForbidden());
            verify(rolesDe(AUTRE), never()).add(anyList());
        }

        @Test
        @DisplayName("Un SUPER_ADMIN crée un rôle de métier (201) et l'attribue à autrui (200)")
        void superAdmin_creeEtAttribue() throws Exception {
            porte(MOI, superAdmin);
            existe("AUDITEUR");
            when(rolesDe(AUTRE).listAll()).thenReturn(List.of());

            mvc.perform(post("/api/v1/roles").with(gestionnaire()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"AUDITEUR\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("AUDITEUR"));
            verify(rolesDuRealm()).create(any(RoleRepresentation.class));

            mvc.perform(post("/api/v1/roles/assign-roles").param("userId", AUTRE).with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON).content("[\"AUDITEUR\"]"))
                    .andExpect(status().isOk());
            verify(rolesDe(AUTRE)).add(anyList());
        }

        @Test
        @DisplayName("Même un SUPER_ADMIN ne crée pas de rôle Keycloak au nom d'une permission : 403")
        void superAdmin_nomReserve_refuse() throws Exception {
            porte(MOI, superAdmin);

            mvc.perform(post("/api/v1/roles").with(gestionnaire()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"MANAGE_USER\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("réservé")));
            verify(rolesDuRealm(), never()).create(any());
        }

        @Test
        @DisplayName("La liste reste lisible par tout compte connecté, pas par un anonyme")
        void lecture() throws Exception {
            when(rolesDuRealm().list()).thenReturn(List.of());
            mvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/roles").with(connecte())).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Attribution d'un rôle applicatif (/api/v1/app-roles/assign)")
    class Attribution {

        @Test
        @DisplayName("401 sans jeton, 403 sans ROLE_MANAGE")
        void acces() throws Exception {
            mvc.perform(post("/api/v1/app-roles/assign").param("userId", AUTRE).param("roleId", lecteur.getId().toString()))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/app-roles/assign").param("userId", AUTRE).param("roleId", lecteur.getId().toString())
                            .with(connecte()))
                    .andExpect(status().isForbidden());
            verify(affectations, never()).save(any());
        }

        @Test
        @DisplayName("Un rôle dont je détiens toutes les permissions : attribué")
        void roleCouvert_attribue() throws Exception {
            when(roles.findById(lecteur.getId())).thenReturn(Optional.of(lecteur));

            mvc.perform(post("/api/v1/app-roles/assign").param("userId", AUTRE).param("roleId", lecteur.getId().toString())
                            .with(gestionnaire()))
                    .andExpect(status().isOk());
            verify(affectations).save(any(UserRoleAssignment.class));
        }

        @Test
        @DisplayName("Un rôle portant une permission que je n'ai pas : 403, la permission nommée")
        void roleNonCouvert_refuse() throws Exception {
            when(roles.findById(licence.getId())).thenReturn(Optional.of(licence));

            mvc.perform(post("/api/v1/app-roles/assign").param("userId", AUTRE).param("roleId", licence.getId().toString())
                            .with(gestionnaire()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("licence-write")));
            verify(affectations, never()).save(any());
        }

        @Test
        @DisplayName("SUPER_ADMIN ne s'attribue que par un SUPER_ADMIN")
        void superAdmin_refuse() throws Exception {
            when(roles.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));

            mvc.perform(post("/api/v1/app-roles/assign").param("userId", AUTRE).param("roleId", superAdmin.getId().toString())
                            .with(gestionnaire()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("SUPER_ADMIN")));
        }

        @Test
        @DisplayName("Nul ne s'attribue un rôle à lui-même, fût-il couvert")
        void soiMeme_refuse() throws Exception {
            when(roles.findById(lecteur.getId())).thenReturn(Optional.of(lecteur));

            mvc.perform(post("/api/v1/app-roles/assign").param("userId", MOI).param("roleId", lecteur.getId().toString())
                            .with(gestionnaire()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("propre compte")));
            verify(affectations, never()).save(any());
        }

        @Test
        @DisplayName("Un SUPER_ADMIN attribue n'importe quel rôle à autrui")
        void superAdmin_attribue() throws Exception {
            porte(MOI, superAdmin);
            when(roles.findById(licence.getId())).thenReturn(Optional.of(licence));

            mvc.perform(post("/api/v1/app-roles/assign").param("userId", AUTRE).param("roleId", licence.getId().toString())
                            .with(gestionnaire()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Un rôle inconnu : 404 et non plus 500")
        void roleInconnu() throws Exception {
            mvc.perform(post("/api/v1/app-roles/assign").param("userId", AUTRE).param("roleId", UUID.randomUUID().toString())
                            .with(gestionnaire()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Contenu d'un rôle applicatif (/api/v1/app-roles)")
    class Contenu {

        @Test
        @DisplayName("Ajouter à mon rôle une permission que je n'ai pas : 403")
        void ajoutNonDetenu_refuse() throws Exception {
            when(roles.findById(gestionnaireDesRoles.getId())).thenReturn(Optional.of(gestionnaireDesRoles));
            String corps = "{\"id\":\"" + gestionnaireDesRoles.getId() + "\",\"name\":\"GESTIONNAIRE_ROLES\","
                    + "\"permissions\":[\"ROLE_MANAGE\",\"nc-read\",\"licence-write\"]}";

            mvc.perform(post("/api/v1/app-roles").with(gestionnaire())
                            .contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("licence-write")));
            verify(roles, never()).save(any());
        }

        @Test
        @DisplayName("Créer un rôle avec des permissions que je détiens : accepté")
        void creationCouverte() throws Exception {
            when(roles.findAllByNameIgnoreCase("CONSULTANT")).thenReturn(List.of());

            mvc.perform(post("/api/v1/app-roles").with(gestionnaire()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"CONSULTANT\",\"permissions\":[\"nc-read\"]}"))
                    .andExpect(status().isOk());
            verify(roles).save(any(AppRole.class));
        }

        @Test
        @DisplayName("Créer un rôle : 403 sans ROLE_MANAGE")
        void creationSansPermission() throws Exception {
            mvc.perform(post("/api/v1/app-roles").with(connecte()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"CONSULTANT\",\"permissions\":[\"nc-read\"]}"))
                    .andExpect(status().isForbidden());
        }
    }
}
