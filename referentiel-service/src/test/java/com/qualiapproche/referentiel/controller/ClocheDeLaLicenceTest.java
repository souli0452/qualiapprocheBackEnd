package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.NotificationDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La cloche de la licence : à qui elle sonne, quand, et avec quelle urgence.
 *
 * <p>Elle ne sonne que pour qui peut renouveler (permission {@code licence-write}), et seulement
 * dans la fenêtre des jalons : avant, elle serait du bruit ; après le terme, elle devient
 * urgente.</p>
 */
class ClocheDeLaLicenceTest {

    private final LicenceInstalleeService licence = mock(LicenceInstalleeService.class);
    private final PermissionChecker permissions = mock(PermissionChecker.class);
    private NotificationsController cloche;

    @BeforeEach
    void preparer() {
        cloche = new NotificationsController(licence, permissions);
        ReflectionTestUtils.setField(cloche, "preavisJours", 3);
        ReflectionTestUtils.setField(cloche, "jalons", "30,15");
        when(permissions.detient("licence-write")).thenReturn(true);
    }

    private List<NotificationDto> sonner() {
        return cloche.notifications().getBody().getData();
    }

    private void restants(long jours, String message) {
        when(licence.etat()).thenReturn(EtatLicenceDto.builder().statut(jours < 0 ? "EXPIREE" : "ACTIVE")
                .joursRestants(jours).message(message).modules(List.of()).build());
    }

    @Test
    @DisplayName("Qui ne peut pas renouveler n'entend rien, et la licence n'est même pas lue")
    void sansLaPermission() {
        when(permissions.detient("licence-write")).thenReturn(false);

        assertThat(sonner()).isEmpty();
        verifyNoInteractions(licence);
    }

    @Test
    @DisplayName("Aucune licence, ou état illisible : silence, l'écran dédié le dit déjà")
    void absenteOuIllisible() {
        when(licence.etat()).thenReturn(EtatLicenceDto.builder().statut("ABSENTE").build());
        assertThat(sonner()).isEmpty();

        when(licence.etat()).thenThrow(new IllegalStateException("base injoignable"));
        assertThat(sonner()).isEmpty();
    }

    @Test
    @DisplayName("Licence échue : une alerte urgente, portant la phrase du service")
    void expiree() {
        restants(-2, "Votre licence a pris fin le 08/10/2026.");

        assertThat(sonner()).singleElement().satisfies(n -> {
            assertThat(n.getCode()).isEqualTo(NotificationsController.CODE_LICENCE_EXPIREE);
            assertThat(n.getGravite()).isEqualTo(GraviteNotification.URGENT);
            assertThat(n.getDetail()).isEqualTo("Votre licence a pris fin le 08/10/2026.");
        });
    }

    @Test
    @DisplayName("Hors de la fenêtre du jalon le plus lointain : rien")
    void horsFenetre() {
        restants(31, null);

        assertThat(sonner()).isEmpty();
    }

    @Test
    @DisplayName("Dans la fenêtre : « attention » ; dans le préavis : « urgent »")
    void gravite() {
        restants(30, null);
        assertThat(sonner()).singleElement().satisfies(n -> {
            assertThat(n.getCode()).isEqualTo(NotificationsController.CODE_LICENCE_BIENTOT_EXPIREE);
            assertThat(n.getGravite()).isEqualTo(GraviteNotification.ATTENTION);
            assertThat(n.getDetail()).isEqualTo("Il reste 30 jours.");
        });

        restants(3, " ");
        assertThat(sonner()).singleElement()
                .satisfies(n -> assertThat(n.getGravite()).isEqualTo(GraviteNotification.URGENT));
    }

    @Test
    @DisplayName("Le dernier jour et la veille se disent en toutes lettres")
    void derniersJours() {
        restants(1, null);
        assertThat(sonner().get(0).getDetail()).isEqualTo("Il reste un jour.");

        restants(0, null);
        assertThat(sonner().get(0).getDetail()).isEqualTo("Elle prend fin aujourd'hui.");
    }

    @Test
    @DisplayName("Un jalon illisible est ignoré ; sans jalon lisible, seul le préavis compte")
    void jalonsIllisibles() {
        ReflectionTestUtils.setField(cloche, "jalons", "trente, ,");

        restants(4, null);
        assertThat(sonner()).isEmpty();

        restants(3, null);
        assertThat(sonner()).hasSize(1);
    }
}
