package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.enumeration.ModuleAbonnement;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.LicenceInstallee;
import com.qualiapproche.referentiel.repository.LicenceInstalleeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'essai gratuit : sept jours, les modules proposés à la vente, une seule fois.
 *
 * <p>Il n'est pas signé — personne dans le produit ne détient la clé de l'éditeur — et c'est
 * précisément pourquoi il est court et unique.</p>
 *
 * <p>Les modules viennent d'un réglage, non de {@link ModuleAbonnement} : l'énumération contient
 * des modules encore à venir, et les ouvrir tous ferait juger l'application sur des écrans vides,
 * puis vivre comme un retrait ce qui n'a jamais été acheté. Un nom inconnu du réglage est écarté
 * plutôt qu'enregistré : il serait sinon comparé sans fin à ceux qu'exige la passerelle, et le
 * module resterait fermé tout l'essai sans un message.</p>
 */
class EssaiGratuitTest {

    private LicenceInstalleeRepository repository;
    private LicenceInstalleeService service;

    @BeforeEach
    void setUp() {
        repository = mock(LicenceInstalleeRepository.class);
        // L'essai n'a pas de destinataire : le code de l'installation ne le concerne pas, et un
        // composant sans code déclaré laisse tout passer.
        service = new LicenceInstalleeService(repository,
                new CodeDeLInstallation(mock(com.qualiapproche.referentiel.repository.StructureRepository.class)));
        ReflectionTestUtils.setField(service, "joursDEssai", 7);
        ReflectionTestUtils.setField(service, "clePublique", "");
        ReflectionTestUtils.setField(service, "modulesDEssai", "NON_CONFORMITE,DOCUMENTAIRE");

        when(repository.existsByType("ESSAI")).thenReturn(false);
        when(repository.save(any(LicenceInstallee.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    /** Le service relit l'état juste après avoir enregistré : on lui rend ce qu'il vient de poser. */
    private void enregistreEtRelit() {
        when(repository.save(any(LicenceInstallee.class))).thenAnswer(appel -> {
            LicenceInstallee essai = appel.getArgument(0);
            when(repository.findTopByOrderByInstalleeLeDesc()).thenReturn(Optional.of(essai));
            return essai;
        });
    }

    @Test
    @DisplayName("L'essai ouvre les modules du réglage, et eux seuls")
    void essai_ouvreLesModulesDuReglage() {
        enregistreEtRelit();

        EtatLicenceDto etat = service.demarrerEssai();

        assertThat(etat.getModules())
                .containsExactlyInAnyOrder(ModuleAbonnement.NON_CONFORMITE.name(),
                        ModuleAbonnement.DOCUMENTAIRE.name());
        assertThat(etat.getStatut()).isEqualTo("ACTIVE");
        assertThat(etat.isActionsOuvertes()).isTrue();
        assertThat(etat.getType()).isEqualTo("ESSAI");
    }

    @Test
    @DisplayName("Le message porte la date de fin, et laisse la liste des modules à son écran")
    void essai_messageDitLaDateSansListerLesModules() {
        enregistreEtRelit();

        EtatLicenceDto etat = service.demarrerEssai();

        // La liste des modules a fait de ce message une ligne de deux cents caractères que
        // personne ne lisait dans le bandeau. Elle a son écran ; le bandeau garde la date, qui
        // est ce sur quoi on décide.
        assertThat(etat.getMessage())
                .contains(LocalDate.now().plusDays(7)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .doesNotContain("Tous les modules")
                .doesNotContain(ModuleAbonnement.DOCUMENTAIRE.name());
        // Les modules restent portés par l'état — c'est l'écran de licence qui les affiche.
        assertThat(etat.getModules()).contains(ModuleAbonnement.DOCUMENTAIRE.name());
    }

    @Test
    @DisplayName("Un module mal orthographié en configuration est écarté, pas enregistré")
    void essai_ecarteUnModuleInconnu() {
        ReflectionTestUtils.setField(service, "modulesDEssai", "NON_CONFORMITE, DOCUMENTAIR");
        enregistreEtRelit();

        EtatLicenceDto etat = service.demarrerEssai();

        // « DOCUMENTAIR » n'aurait jamais correspondu à ce que la passerelle exige : le module
        // serait resté fermé tout l'essai, sans le moindre message.
        assertThat(etat.getModules()).containsExactly(ModuleAbonnement.NON_CONFORMITE.name());
    }

    @Test
    @DisplayName("Un réglage qui n'ouvre rien refuse l'essai plutôt que d'en poser un vide")
    void essai_refuseUnReglageVide() {
        ReflectionTestUtils.setField(service, "modulesDEssai", "MODULE_INEXISTANT");

        assertThatThrownBy(() -> service.demarrerEssai())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aucun module");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Il court sept jours à compter d'aujourd'hui")
    void essai_dureSeptJours() {
        enregistreEtRelit();

        EtatLicenceDto etat = service.demarrerEssai();

        assertThat(etat.getDebut()).isEqualTo(LocalDate.now());
        assertThat(etat.getFin()).isEqualTo(LocalDate.now().plusDays(7));
    }

    @Test
    @DisplayName("Une seule fois par installation : sinon l'essai remplacerait l'abonnement")
    void essai_neSeRedemandePas() {
        when(repository.existsByType("ESSAI")).thenReturn(true);

        assertThatThrownBy(() -> service.demarrerEssai())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà été utilisé");

        verify(repository, never()).save(any());
    }
}
