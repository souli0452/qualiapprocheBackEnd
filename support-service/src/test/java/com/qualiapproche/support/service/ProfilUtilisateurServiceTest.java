package com.qualiapproche.support.service;

import com.qualiapproche.support.client.UserClient;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La visibilité d'un document repose entièrement sur ce profil : la structure de rattachement de
 * l'appelant et ses rôles. Ce que ces tests fixent, c'est surtout le comportement en cas de
 * défaillance — un profil qui reviendrait « vide » de façon inattendue restreint l'accès, un profil
 * qui reviendrait à tort avec le rôle qualité l'ouvrirait en grand.
 */
class ProfilUtilisateurServiceTest {

    private static final String UTILISATEUR = "9f1c2d3e-0000-4000-8000-000000000001";
    private static final String STRUCTURE = "8a7b6c5d-0000-4000-8000-000000000002";

    private UserClient client;
    private ProfilUtilisateurService service;

    @BeforeEach
    void setUp() {
        client = mock(UserClient.class);
        service = new ProfilUtilisateurService(client);
        ReflectionTestUtils.setField(service, "retentionSecondes", 60L);
        ReflectionTestUtils.setField(service, "tailleMax", 100);
        authentifier(UTILISATEUR);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authentifier(String userId) {
        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject(userId)
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    @Test
    @DisplayName("Structure et rôles sont lus depuis user-service")
    void profil_litStructureEtRoles() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE,
                "roles", List.of("PILOTE", "AGENT")));

        ProfilUtilisateurService.Profil profil = service.profilCourant();

        assertThat(profil.structureId()).isEqualTo(STRUCTURE);
        assertThat(profil.roles()).containsExactlyInAnyOrder("PILOTE", "AGENT");
        assertThat(profil.estResponsableQualite()).isFalse();
    }

    @Test
    @DisplayName("Le responsable qualité est reconnu, quelle que soit la casse")
    void profil_reconnaitLeResponsableQualite() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE,
                "roles", List.of("responsable_qualite")));

        assertThat(service.profilCourant().estResponsableQualite()).isTrue();
    }

    @Test
    @DisplayName("Le super administrateur voit toutes les structures")
    void superAdmin_voitTout() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE, "roles", List.of("SUPER_ADMIN")));

        // Le test ne portait que sur les rôles techniques du jeton — ADMIN, MANAGE — si bien
        // qu'un super administrateur sans l'un d'eux était borné à sa propre structure.
        assertThat(service.profilCourant().voitToutesLesStructures()).isTrue();
    }

    @Test
    @DisplayName("« SUPERADMIN » sans souligné est reconnu de la même façon")
    void superAdmin_orthographeSansSouligne() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE, "roles", List.of("superadmin")));

        assertThat(service.profilCourant().voitToutesLesStructures()).isTrue();
    }

    @Test
    @DisplayName("Le responsable qualité voit toutes les structures ; un agent, non")
    void porteeSelonLeRole() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE, "roles", List.of("RESPONSABLE_QUALITE")));
        assertThat(service.profilCourant().voitToutesLesStructures()).isTrue();

        service = new ProfilUtilisateurService(client);
        ReflectionTestUtils.setField(service, "retentionSecondes", 60L);
        ReflectionTestUtils.setField(service, "tailleMax", 100);
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE, "roles", List.of("AGENT", "PILOTE")));
        assertThat(service.profilCourant().voitToutesLesStructures()).isFalse();
    }

    @Test
    @DisplayName("user-service injoignable : profil vide, donc accès restreint et non élargi")
    void userServiceIndisponible_profilVide() {
        when(client.getUserById(anyString())).thenThrow(new IllegalStateException("injoignable"));

        ProfilUtilisateurService.Profil profil = service.profilCourant();

        // Structure nulle : la clause de visibilité s'y réduit d'elle-même, et l'utilisateur
        // retombe sur ses propres documents et ses partages nominatifs.
        assertThat(profil.structureId()).isNull();
        assertThat(profil.estResponsableQualite()).isFalse();
    }

    @Test
    @DisplayName("Un utilisateur sans structure de rattachement ne se voit prêter aucune structure")
    void utilisateurSansStructure() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of("roles", List.of("AGENT")));

        assertThat(service.profilCourant().structureId()).isNull();
    }

    @Test
    @DisplayName("Le profil est mis en cache : une rafale de contrôles n'interroge user-service qu'une fois")
    void profil_misEnCache() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of("structure", STRUCTURE, "roles", List.of()));

        service.profilCourant();
        service.profilCourant();
        service.profilCourant();

        // La visibilité est évaluée à chaque recherche, chaque consultation, chaque téléchargement :
        // sans cache, afficher une liste coûterait autant d'appels que de lignes.
        verify(client, times(1)).getUserById(UTILISATEUR);
    }

    @Test
    @DisplayName("Sans utilisateur authentifié, aucun profil n'est prêté")
    void sansUtilisateur_profilVide() {
        SecurityContextHolder.clearContext();

        assertThat(service.profilCourant().structureId()).isNull();
        assertThat(service.profilCourant().roles()).isEmpty();
    }
}
