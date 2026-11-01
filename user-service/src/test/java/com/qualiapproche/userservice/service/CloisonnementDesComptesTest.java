package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.userservice.client.StructureClient;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import com.qualiapproche.userservice.entities.mappers.KcUserMapper;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Keycloak est partagé par tous les clients : chaque client ne voit, ne modifie et ne compte que
 * les comptes de sa direction ; un appelant sans direction n'en voit aucun ; le système les voit tous.
 */
class CloisonnementDesComptesTest {

    private static final String A = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String B = "bbbbbbbb-0000-0000-0000-000000000002";

    private final Keycloak keycloak = mock(Keycloak.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    private final UsersResource comptes = mock(UsersResource.class);
    private final StructureClient structures = mock(StructureClient.class);
    private final UserRoleAssignmentRepository affectations = mock(UserRoleAssignmentRepository.class);
    private final PlafondDUtilisateurs plafond = mock(PlafondDUtilisateurs.class);
    private final GardeDesHabilitations garde = mock(GardeDesHabilitations.class);
    private KcUserService service;
    private KcAuthProperties proprietes;

    @BeforeEach
    void preparer() {
        proprietes = new KcAuthProperties();
        proprietes.setRealm("quali");
        service = new KcUserService(mock(KcTokenService.class), new KcUserMapper(), keycloak, proprietes,
                mock(AppRoleRepository.class), affectations, structures, mock(SendMailService.class), plafond,
                mock(ProfilPersonnel.class), mock(JetonsDeCompte.class), garde);
        when(keycloak.realm("quali").users()).thenReturn(comptes);
        when(affectations.findByUserId(anyString())).thenReturn(List.of());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private static void connecter(String direction) {
        Jwt.Builder jeton = Jwt.withTokenValue("jeton").header("alg", "RS256").subject("moi")
                .claim("preferred_username", "moi")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (direction != null) {
            jeton.claim("direction_id", direction);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jeton.build(), List.of()));
    }

    private static UserRepresentation compte(String id, String direction) {
        UserRepresentation u = new UserRepresentation();
        u.setId(id);
        u.setUsername(id);
        u.setEmail(id + "@q.bf");
        u.setEnabled(true);
        Map<String, List<String>> attributs = new HashMap<>();
        if (direction != null) {
            attributs.put("direction", List.of(direction));
        }
        u.setAttributes(attributs);
        return u;
    }

    private void brancher(UserRepresentation u) {
        UserResource ressource = mock(UserResource.class);
        when(ressource.toRepresentation()).thenReturn(u);
        when(comptes.get(u.getId())).thenReturn(ressource);
    }

    @Test
    @DisplayName("La liste ne rend que les comptes de la direction de l'appelant")
    void listeBornee() {
        connecter(A);
        when(comptes.list()).thenReturn(List.of(compte("a1", A), compte("b1", B), compte("orphelin", null)));

        assertThat(service.getAllUsers(PageRequest.of(0, 10)).getContent())
                .extracting(KcUserDto::getId).containsExactly("a1");
    }

    @Test
    @DisplayName("Un appelant sans direction (administrateur de la plateforme) ne voit aucun compte")
    void sansDirectionRienAVoir() {
        connecter(null);
        when(comptes.list()).thenReturn(List.of(compte("a1", A), compte("b1", B)));

        assertThat(service.getAllUsers(PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    @DisplayName("Le système (sans jeton) voit tous les comptes")
    void racineVoitTout() {
        when(comptes.list()).thenReturn(List.of(compte("a1", A), compte("b1", B)));

        assertThat(service.getAllUsers(PageRequest.of(0, 10)).getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Le compte d'un autre client est introuvable : ni lu, ni modifié, ni désactivé")
    void compteDUnAutreClient() {
        connecter(A);
        brancher(compte("b1", B));

        for (Runnable geste : List.<Runnable>of(
                () -> service.getUserById("b1"),
                () -> service.changeUserStatus("b1", false),
                () -> service.fixerMotDePasseParGestionnaire("b1", "Nouveau-1"),
                () -> service.updateUser(KcUserDto.builder().id("b1").build()))) {
            assertThatThrownBy(geste::run).isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
        verify(garde, never()).exigerAscendantSur(anyString(), anyString());
    }

    @Test
    @DisplayName("Sans direction, aucun compte n'est créé")
    void creationSansDirection() {
        connecter(null);

        assertThatThrownBy(() -> service.createUser(KcUserDto.builder().username("x").build()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(comptes, never()).create(any());
    }

    @Test
    @DisplayName("Une structure que referentiel ne rend pas au client : 400, aucun compte créé")
    void structureDUnAutreClient() {
        connecter(A);
        when(structures.getStructureById(any())).thenThrow(mock(feign.FeignException.NotFound.class));

        assertThatThrownBy(() -> service.createUser(KcUserDto.builder().username("x")
                .structure(UUID.randomUUID().toString()).build()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(comptes, never()).create(any());
    }

    @Test
    @DisplayName("Referentiel injoignable : la création est refusée plutôt que faite à l'aveugle")
    void referentielInjoignable() {
        connecter(A);
        when(structures.getStructureById(any())).thenThrow(new RuntimeException("connexion refusée"));

        assertThatThrownBy(() -> service.createUser(KcUserDto.builder().username("x")
                .structure(UUID.randomUUID().toString()).build()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(comptes, never()).create(any());
    }

    @Test
    @DisplayName("Le plafond ne compte que les comptes actifs de la direction")
    void plafondParDirection() {
        connecter(A);
        PlafondDUtilisateurs vrai = new PlafondDUtilisateurs(keycloak, proprietes, structures);
        EtatLicenceDto licence = new EtatLicenceDto();
        licence.setUtilisateursMax(2);
        when(structures.etatLicence()).thenReturn(licence);
        when(comptes.list(anyInt(), anyInt())).thenReturn(List.of(compte("a1", A), compte("b1", B),
                compte("b2", B), compte("b3", B)));

        // Un seul compte en A pour un plafond de 2 : la création passe, malgré les trois comptes de B.
        vrai.verifierAvantCreation();
    }

    @Test
    @DisplayName("La direction se lit dans le jeton que Keycloak vient de rendre")
    void directionDuJetonEmis() {
        String corps = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"direction_id\":\"" + A + "\"}").getBytes());

        assertThat(DirectionDesComptes.duJetonFraichementEmis("e30." + corps + ".sig")).hasToString(A);
        assertThat(DirectionDesComptes.duJetonFraichementEmis("pas-un-jeton")).isNull();
    }
}
