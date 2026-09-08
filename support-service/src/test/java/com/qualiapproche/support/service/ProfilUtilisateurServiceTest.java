package com.qualiapproche.support.service;

import com.qualiapproche.common.utils.PermissionsPortee;
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
 * l'appelant et ses <b>permissions</b>. Ce que ces tests fixent, c'est surtout le comportement en
 * cas de défaillance — un profil qui reviendrait « vide » de façon inattendue restreint l'accès, un
 * profil qui reviendrait à tort avec la portée transverse l'ouvrirait en grand.
 *
 * <p>Deux cas ont disparu avec la bascule des noms de rôles vers les permissions : la
 * reconnaissance de « responsable_qualite » quelle que soit la casse, et celle de « SUPERADMIN »
 * sans souligné. Ils n'existaient que parce que le code comparait des noms — une orthographe de
 * plus, un cas de test de plus. Une permission n'a qu'une écriture.</p>
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
    @DisplayName("Structure, rôles et permissions sont lus depuis user-service")
    void profil_litStructureRolesEtPermissions() {
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE,
                "roles", List.of("PILOTE", "AGENT"),
                "permissions", List.of("nc-read", "document-read")));

        ProfilUtilisateurService.Profil profil = service.profilCourant();

        assertThat(profil.structureId()).isEqualTo(STRUCTURE);
        // Les noms de rôles restent lus, pour la seule comparaison au classement documentaire.
        assertThat(profil.roles()).containsExactlyInAnyOrder("PILOTE", "AGENT");
        assertThat(profil.permissions()).containsExactlyInAnyOrder("nc-read", "document-read");
        assertThat(profil.voitToutesLesStructures()).isFalse();
    }

    @Test
    @DisplayName("La permission ouvre la portée transverse, quel que soit le nom du rôle qui la porte")
    void permission_ouvreLaPorteeTransverse() {
        // Le nom du rôle n'entre plus en ligne de compte : une organisation nomme les siens, et
        // celui-ci n'est aucun de ceux que le code connaissait.
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE,
                "roles", List.of("COORDONNATEUR_QUALITE"),
                "permissions", List.of(PermissionsPortee.TOUTES_STRUCTURES)));

        assertThat(service.profilCourant().voitToutesLesStructures()).isTrue();
    }

    @Test
    @DisplayName("Un rôle jadis privilégié par son nom ne l'est plus sans la permission")
    void nomDeRoleSeul_nOuvrePlusRien() {
        // Le cœur de la bascule : porter le nom ne suffit plus, c'est la dotation qui décide.
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE,
                "roles", List.of("SUPER_ADMIN", "RESPONSABLE_QUALITE"),
                "permissions", List.of("nc-read")));

        assertThat(service.profilCourant().voitToutesLesStructures()).isFalse();
    }

    @Test
    @DisplayName("Voir toutes les structures n'emporte pas la dispense de classement")
    void porteeTransverse_neDispensePasDuClassement() {
        // Le responsable qualité voit toutes les structures, pas tous les classements : deux
        // permissions distinctes, et les confondre ouvrirait les documents réservés.
        when(client.getUserById(UTILISATEUR)).thenReturn(Map.of(
                "structure", STRUCTURE,
                "permissions", List.of(PermissionsPortee.TOUTES_STRUCTURES)));

        ProfilUtilisateurService.Profil profil = service.profilCourant();

        assertThat(profil.voitToutesLesStructures()).isTrue();
        assertThat(profil.estAdministrateur()).isFalse();
    }

    @Test
    @DisplayName("user-service injoignable : profil vide, donc accès restreint et non élargi")
    void userServiceIndisponible_profilVide() {
        when(client.getUserById(anyString())).thenThrow(new IllegalStateException("injoignable"));

        ProfilUtilisateurService.Profil profil = service.profilCourant();

        // Structure nulle : la clause de visibilité s'y réduit d'elle-même, et l'utilisateur
        // retombe sur ses propres documents et ses partages nominatifs.
        assertThat(profil.structureId()).isNull();
        assertThat(profil.voitToutesLesStructures()).isFalse();
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
