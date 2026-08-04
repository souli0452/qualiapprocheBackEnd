package com.qualiapproche.workflow.service;

import com.qualiapproche.workflow.client.UserRoleClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un rôle peut être désigné, sur une étape comme au catalogue, par son <b>nom</b> ou par son
 * <b>identifiant</b> : les deux formes coexistent en base selon l'écran et l'époque de la saisie.
 *
 * <p>C'est cette tolérance qui rend inutile toute reprise de données — sans elle, il faudrait
 * convertir les valeurs héritées d'une base à l'autre, à travers deux services, sans transaction
 * commune. Elle n'est vérifiée nulle part à l'exécution : la resserrer sur une seule forme
 * n'échouerait ici sur rien, et se paierait ailleurs — une étape dont plus personne n'est habilité
 * à décider.</p>
 */
class RolesUtilisateurServiceTest {

    private static final String UTILISATEUR = "9f1c2d3e-0000-4000-8000-000000000001";
    private static final String IDENTIFIANT_ROLE = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    private UserRoleClient client;
    private RolesUtilisateurService service;

    @BeforeEach
    void setUp() {
        client = mock(UserRoleClient.class);
        service = new RolesUtilisateurService(client);
        ReflectionTestUtils.setField(service, "retentionSecondes", 60L);
        ReflectionTestUtils.setField(service, "tailleMax", 100);

        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject(UTILISATEUR)
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void userServiceRepond(Map<String, String>... roles) {
        when(client.getMyRoles()).thenReturn(Map.of("data", List.of(roles)));
    }

    @Test
    @DisplayName("Les rôles de l'utilisateur sont retenus sous leurs deux formes : nom et identifiant")
    @SuppressWarnings("unchecked")
    void roles_retenusParNomEtParIdentifiant() {
        userServiceRepond(Map.of("id", IDENTIFIANT_ROLE, "name", "PILOTE"));

        Set<String> roles = service.rolesDeLUtilisateurCourant();

        // Une étape qui désigne « PILOTE » et une entrée de catalogue plus ancienne qui porte
        // encore l'identifiant du rôle mènent l'une comme l'autre au même titulaire.
        assertThat(roles).contains("PILOTE", IDENTIFIANT_ROLE.toUpperCase());
    }

    @Test
    @DisplayName("Une réponse sans identifiant ou sans nom ne fait pas échouer la résolution")
    @SuppressWarnings("unchecked")
    void roles_tolerentUneFormeManquante() {
        userServiceRepond(Map.of("name", "RESPONSABLE_QUALITE"), Map.of("id", IDENTIFIANT_ROLE));

        assertThat(service.rolesDeLUtilisateurCourant())
                .contains("RESPONSABLE_QUALITE", IDENTIFIANT_ROLE.toUpperCase());
    }

    @Test
    @DisplayName("user-service injoignable : aucun rôle, donc aucune transition restreinte autorisée")
    void userServiceIndisponible_aucunRole() {
        when(client.getMyRoles()).thenThrow(new IllegalStateException("user-service injoignable"));

        assertThat(service.rolesDeLUtilisateurCourant()).isEmpty();
    }

    @Test
    @DisplayName("Sans utilisateur identifié, aucun rôle n'est prêté")
    void sansUtilisateur_aucunRole() {
        SecurityContextHolder.clearContext();

        assertThat(service.rolesDeLUtilisateurCourant()).isEmpty();
    }
}
