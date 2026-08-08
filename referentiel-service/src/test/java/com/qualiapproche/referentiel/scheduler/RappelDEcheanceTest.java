package com.qualiapproche.referentiel.scheduler;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.referentiel.client.UserClient;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les rappels d'échéance de la licence.
 *
 * <p>Trois exigences, et une seule raison derrière les trois : que quelqu'un puisse agir à
 * temps. Un rappel envoyé une seule fois tombe un jour de congé ; un rappel sans date n'appelle
 * aucune décision ; un rappel adressé à une boîte générique est relevé par quelqu'un qui ne peut
 * rien y faire.</p>
 */
class RappelDEcheanceTest {

    private LicenceInstalleeService licence;
    private StructureRepository structures;
    private UserClient utilisateurs;
    private SendMailService courriels;
    private LicenseScheduler scheduler;

    @BeforeEach
    void setUp() {
        licence = mock(LicenceInstalleeService.class);
        structures = mock(StructureRepository.class);
        utilisateurs = mock(UserClient.class);
        courriels = mock(SendMailService.class);

        scheduler = new LicenseScheduler(licence, structures, utilisateurs, courriels);
        ReflectionTestUtils.setField(scheduler, "preavisJours", 3);
        ReflectionTestUtils.setField(scheduler, "jalons", "30,15");

        when(utilisateurs.getUsersByRole("SUPER_ADMIN")).thenReturn(List.of(
                DestinataireDto.builder().email("admin@exemple.fr").nomComplet("Admin").build()));
    }

    private void licenceExpirantDans(long jours) {
        when(licence.etat()).thenReturn(EtatLicenceDto.builder()
                .statut(jours >= 0 ? "ACTIVE" : "EXPIREE")
                .actionsOuvertes(jours >= 0)
                .type("COMMERCIALE")
                .joursRestants(jours)
                .fin(LocalDate.now().plusDays(jours))
                .modules(List.of("NON_CONFORMITE"))
                .build());
    }

    private String dernierMessage() {
        ArgumentCaptor<String> corps = ArgumentCaptor.forClass(String.class);
        verify(courriels).sendMail(anyString(), anyString(), corps.capture(), anyBoolean());
        return corps.getValue();
    }

    @Test
    @DisplayName("Un rappel chaque jour sur les trois derniers, jour du terme compris")
    void rappel_chaqueJourDuPreavis() {
        for (long jours : new long[] {3, 2, 1, 0}) {
            licenceExpirantDans(jours);
            scheduler.checkLicenseExpirations();
        }

        verify(courriels, times(4)).sendMail(eq("admin@exemple.fr"), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Un rappel isolé à J-30 et J-15 : un renouvellement payant demande du temps")
    void rappel_auxJalonsLointains() {
        for (long jours : new long[] {30, 15}) {
            licenceExpirantDans(jours);
            scheduler.checkLicenseExpirations();
        }

        verify(courriels, times(2)).sendMail(eq("admin@exemple.fr"), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Aucun rappel entre les jalons : trop fréquent, il finirait filtré")
    void rappel_pasEntreLesJalons() {
        for (long jours : new long[] {29, 16, 14, 4}) {
            licenceExpirantDans(jours);
            scheduler.checkLicenseExpirations();
        }

        verify(courriels, never()).sendMail(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Le message porte la date d'expiration, pas seulement « bientôt »")
    void rappel_porteLaDate() {
        licenceExpirantDans(2);

        scheduler.checkLicenseExpirations();

        assertThat(dernierMessage()).contains(
                LocalDate.now().plusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Test
    @DisplayName("Il s'adresse aux super administrateurs, seuls à pouvoir poser une licence")
    void rappel_vaAuxSuperAdmins() {
        when(utilisateurs.getUsersByRole("SUPER_ADMIN")).thenReturn(List.of(
                DestinataireDto.builder().email("un@exemple.fr").build(),
                DestinataireDto.builder().email("deux@exemple.fr").build()));
        licenceExpirantDans(1);

        scheduler.checkLicenseExpirations();

        verify(courriels).sendMail(eq("un@exemple.fr"), anyString(), anyString(), anyBoolean());
        verify(courriels).sendMail(eq("deux@exemple.fr"), anyString(), anyString(), anyBoolean());
        verify(structures, never()).findAllByTypeStructure(any());
    }

    @Test
    @DisplayName("Aucun administrateur joignable : l'adresse de la direction sert de repli")
    void rappel_repliSurLaDirection() {
        // Sans ce repli, l'alerte disparaîtrait en silence et l'échéance surviendrait sans que
        // personne n'ait été prévenu.
        when(utilisateurs.getUsersByRole("SUPER_ADMIN")).thenThrow(new IllegalStateException("injoignable"));
        when(structures.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of(
                Structure.builder().email("direction@exemple.fr").build()));
        licenceExpirantDans(0);

        scheduler.checkLicenseExpirations();

        verify(courriels).sendMail(eq("direction@exemple.fr"), anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Licence déjà expirée ou absente : plus de rappel, la fenêtre le dit déjà")
    void rappel_pasApresLeTerme() {
        licenceExpirantDans(-2);
        scheduler.checkLicenseExpirations();

        when(licence.etat()).thenReturn(EtatLicenceDto.builder()
                .statut("ABSENTE").actionsOuvertes(false).modules(List.of()).build());
        scheduler.checkLicenseExpirations();

        verify(courriels, never()).sendMail(anyString(), anyString(), anyString(), anyBoolean());
    }
}
