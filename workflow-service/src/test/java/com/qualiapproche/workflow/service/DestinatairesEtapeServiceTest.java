package com.qualiapproche.workflow.service;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.workflow.client.UserRoleClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Résolution des destinataires d'une étape auprès de user-service.
 *
 * <p>C'est ce maillon qui manquait : l'adresse était fabriquée à partir du nom du rôle et ne
 * correspondait à aucune boîte réelle.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DestinatairesEtapeServiceTest {

    @Mock private UserRoleClient userRoleClient;

    private DestinatairesEtapeService service;

    @BeforeEach
    void setUp() {
        service = new DestinatairesEtapeService(userRoleClient);
        ReflectionTestUtils.setField(service, "retentionSecondes", 120L);
    }

    private Map<String, Object> reponse(Map<String, String>... utilisateurs) {
        return Map.of("data", List.of(utilisateurs));
    }

    @Test
    @DisplayName("Les porteurs du rôle sont résolus avec leur adresse et leur nom")
    void roleResolu_destinatairesRendus() {
        when(userRoleClient.getUsersByRole("VERIFICATEUR")).thenReturn(reponse(
                Map.of("userId", "u1", "email", "claire@exemple.fr", "nomComplet", "Claire Martin"),
                Map.of("userId", "u2", "email", "sam@exemple.fr", "nomComplet", "Sam Diop")));

        List<DestinataireDto> aDestinataires = service.destinatairesDuRole("VERIFICATEUR");

        assertThat(aDestinataires).hasSize(2);
        assertThat(aDestinataires).extracting(DestinataireDto::getEmail)
                .containsExactly("claire@exemple.fr", "sam@exemple.fr");
        assertThat(aDestinataires.getFirst().getNomComplet()).isEqualTo("Claire Martin");
    }

    @Test
    @DisplayName("Un utilisateur sans adresse est écarté : il n'est pas joignable")
    void utilisateurSansAdresse_ecarte() {
        when(userRoleClient.getUsersByRole(anyString())).thenReturn(reponse(
                Map.of("userId", "u1", "email", "claire@exemple.fr", "nomComplet", "Claire Martin"),
                Map.of("userId", "u2", "nomComplet", "Sans Adresse")));

        assertThat(service.destinatairesDuRole("VERIFICATEUR"))
                .extracting(DestinataireDto::getEmail)
                .containsExactly("claire@exemple.fr");
    }

    @Test
    @DisplayName("À défaut de nom, l'adresse sert de libellé")
    void sansNom_adresseCommeLibelle() {
        when(userRoleClient.getUsersByRole(anyString())).thenReturn(reponse(
                Map.of("userId", "u1", "email", "claire@exemple.fr")));

        assertThat(service.destinatairesDuRole("VERIFICATEUR").getFirst().getNomComplet())
                .isEqualTo("claire@exemple.fr");
    }

    @Test
    @DisplayName("Un rôle que personne ne porte ne rend aucun destinataire")
    void rolePersonne_listeVide() {
        when(userRoleClient.getUsersByRole(anyString())).thenReturn(Map.of("data", List.of()));

        assertThat(service.destinatairesDuRole("ROLE_ORPHELIN")).isEmpty();
    }

    @Test
    @DisplayName("Un rôle absent ou vide n'interroge même pas user-service")
    void roleAbsent_aucunAppel() {
        assertThat(service.destinatairesDuRole(null)).isEmpty();
        assertThat(service.destinatairesDuRole("  ")).isEmpty();

        verify(userRoleClient, times(0)).getUsersByRole(anyString());
    }

    // ------------------------------------------------------------------ cache et résilience

    @Test
    @DisplayName("Les appels répétés pour un même rôle n'interrogent user-service qu'une fois")
    void appelsRepetes_uneSeuleInterrogation() {
        when(userRoleClient.getUsersByRole(anyString())).thenReturn(reponse(
                Map.of("userId", "u1", "email", "claire@exemple.fr", "nomComplet", "Claire Martin")));

        for (int i = 0; i < 10; i++) {
            service.destinatairesDuRole("VERIFICATEUR");
        }

        // Une étape est franchie souvent, les affectations changent rarement.
        verify(userRoleClient, times(1)).getUsersByRole("VERIFICATEUR");
    }

    @Test
    @DisplayName("La casse du rôle ne démultiplie pas les interrogations")
    void casseDuRole_memeEntreeDeCache() {
        when(userRoleClient.getUsersByRole(anyString())).thenReturn(reponse(
                Map.of("userId", "u1", "email", "claire@exemple.fr")));

        service.destinatairesDuRole("verificateur");
        service.destinatairesDuRole("VERIFICATEUR");

        verify(userRoleClient, times(1)).getUsersByRole(anyString());
    }

    @Test
    @DisplayName("user-service injoignable : la dernière liste connue est réutilisée")
    void serviceInjoignable_derniereListeConnue() {
        when(userRoleClient.getUsersByRole(anyString())).thenReturn(reponse(
                Map.of("userId", "u1", "email", "claire@exemple.fr", "nomComplet", "Claire Martin")));
        service.destinatairesDuRole("VERIFICATEUR");

        // L'entrée expire, et l'interrogation suivante échoue.
        ReflectionTestUtils.setField(service, "retentionSecondes", -1L);
        doThrow(new IllegalStateException("user-service indisponible"))
                .when(userRoleClient).getUsersByRole(anyString());

        assertThat(service.destinatairesDuRole("VERIFICATEUR"))
                .as("une indisponibilité passagère ne doit pas faire disparaître la notification")
                .extracting(DestinataireDto::getEmail)
                .containsExactly("claire@exemple.fr");
    }

    @Test
    @DisplayName("user-service injoignable sans rien en cache : aucun destinataire, sans exception")
    void serviceInjoignableSansCache_listeVide() {
        doThrow(new IllegalStateException("user-service indisponible"))
                .when(userRoleClient).getUsersByRole(anyString());

        assertThat(service.destinatairesDuRole("VERIFICATEUR")).isEmpty();
    }

    @Test
    @DisplayName("Une réponse de forme inattendue ne fait pas échouer la résolution")
    void reponseInattendue_listeVide() {
        when(userRoleClient.getUsersByRole(anyString())).thenReturn(Map.of("data", "pas une liste"));

        assertThat(service.destinatairesDuRole("VERIFICATEUR")).isEmpty();
    }
}
