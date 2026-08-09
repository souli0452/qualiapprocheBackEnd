package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.referentiel.entities.LicenceInstallee;
import com.qualiapproche.referentiel.repository.LicenceInstalleeRepository;
import com.qualiapproche.referentiel.repository.StructureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Aucune licence sans signature.
 *
 * <p>Un jeton absent valait « essai local, rien à vérifier ». Le raccourci n'était vrai que tant
 * que le produit était seul à créer des lignes sans jeton : la signature <b>et</b> le contrôle du
 * code partenaire étaient alors sautés, et la validité lue directement dans les colonnes. Une
 * ligne insérée à la main — type commercial, jeton nul, terme en 2099, tous les modules — passait
 * donc pour une licence perpétuelle, sans rien connaître de la cryptographie.</p>
 *
 * <p>Une ligne sans jeton ne peut plus venir que d'une écriture directe en base : elle n'ouvre
 * rien. L'essai lui-même est désormais émis signé par l'éditeur, comme toute autre licence — c'est
 * ce qui permet de le compter, ce qu'une installation ne pouvait pas faire pour elle-même : il
 * suffisait d'effacer la ligne pour en obtenir un nouveau.</p>
 */
class LicenceNonSigneeTest {

    /** Clé publique de l'éditeur, telle que la configure referentiel-service. */
    private static final String CLE_PUBLIQUE =
            "MCowBQYDK2VwAyEAs2ajPoJ/faSW2FTtOV9tVXc/k3Fk2ZNc3SwQsyEJbxg";

    /** Licence réellement émise par l'outil pour le CHU du Burkina Faso — code {@code CHU-BF}. */
    private static final String LICENCE_DU_CHU =
            "QSL1.eyJyZWYiOiJMSUMtMjAyNi0wMDAxIiwiY2xpIjoiQ0hVLUJGIiwibm9tIjoiQ0hVIGR1IEJ1cmtpbmEg"
            + "RmFzbyIsImRlYiI6IjIwMjYtMDgtMDgiLCJmaW4iOiIyMDI3LTA4LTA4IiwibW9kIjpbIkRPQ1VNRU5UQUlS"
            + "RSIsIk5PTl9DT05GT1JNSVRFIl0sInVzciI6MjUsInR5cCI6IkNPTU1FUkNJQUxFIiwiZWR0IjoiRXNzZW50"
            + "aWVsIn0.qxeikzlSkbN-lngX2D89cFqYzyufZAGavgmvS3mcfJbu1l-UBlDdU76ahm9ZONZlaae-PCWuJPfdqrMoYLc0Dw";

    private LicenceInstalleeRepository repository;
    private CodeDeLInstallation installation;
    private LicenceInstalleeService service;

    @BeforeEach
    void setUp() {
        repository = mock(LicenceInstalleeRepository.class);
        installation = new CodeDeLInstallation(mock(StructureRepository.class));
        service = new LicenceInstalleeService(repository, installation);
        ReflectionTestUtils.setField(service, "clePublique", CLE_PUBLIQUE);
        ReflectionTestUtils.setField(installation, "duDeploiement", "CHU-BF");
        ReflectionTestUtils.setField(installation, "attendu", null);

        when(repository.save(any(LicenceInstallee.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    @DisplayName("Une licence perpétuelle forgée en base n'ouvre rien")
    void licenceForgeeEnBaseNOuvreRien() {
        // Ce qu'un partenaire ayant la main sur sa base écrirait : tout, jusqu'en 2099.
        pose(LicenceInstallee.builder()
                .type("COMMERCIALE")
                .reference("LIC-MAISON")
                .partenaireNom("Écrite à la main")
                .partenaireCode("CHU-BF")
                .jeton(null)
                .debut(LocalDate.now().minusYears(1))
                .fin(LocalDate.of(2099, 12, 31))
                .modules("DOCUMENTAIRE,NON_CONFORMITE")
                .utilisateursMax(9999)
                );

        EtatLicenceDto etat = service.etat();

        assertThat(etat.getStatut()).isEqualTo("ABSENTE");
        assertThat(etat.isActionsOuvertes())
                .as("sans signature, aucune écriture ne doit être ouverte")
                .isFalse();
        assertThat(etat.getModules())
                .as("les modules ne se lisent pas dans une colonne que le client écrit")
                .isEmpty();
    }

    @Test
    @DisplayName("Un essai local hérité n'ouvre plus rien non plus")
    void essaiLocalHeriteNOuvrePlusRien() {
        // Les essais auto-accordés par l'ancienne version : eux aussi sans jeton, et eux aussi
        // reconductibles à volonté par un effacement de ligne.
        pose(LicenceInstallee.builder()
                .type("ESSAI")
                .reference("ESSAI-2026-08-01")
                .partenaireNom("Essai gratuit")
                .jeton(null)
                .debut(LocalDate.now().minusDays(1))
                .fin(LocalDate.now().plusDays(6))
                .modules("DOCUMENTAIRE,NON_CONFORMITE")
                .utilisateursMax(0)
                );

        EtatLicenceDto etat = service.etat();

        assertThat(etat.getStatut()).isEqualTo("ABSENTE");
        assertThat(etat.isActionsOuvertes()).isFalse();
    }

    @Test
    @DisplayName("Un jeton retouché ne passe pas davantage")
    void jetonRetoucheEstRejete() {
        pose(LicenceInstallee.builder()
                .type("COMMERCIALE")
                .reference("LIC-2026-0001")
                // Signature corrompue sur ses derniers caractères : viser une portion de la charge
                // utile ne prouve rien si le motif cherché ne s'y trouve pas — le jeton repart
                // alors intact, et le cas passe sans rien vérifier.
                .jeton(LICENCE_DU_CHU.substring(0, LICENCE_DU_CHU.length() - 4) + "AAAA")
                .debut(LocalDate.now().minusDays(1))
                .fin(LocalDate.now().plusYears(1))
                .modules("DOCUMENTAIRE")
                );

        assertThat(service.etat().getStatut()).isEqualTo("ABSENTE");
    }

    @Test
    @DisplayName("Une licence signée, elle, continue d'ouvrir ce qu'elle porte")
    void licenceSigneeResteValable() {
        // Le durcissement ne doit pas fermer la porte au client légitime : c'est le seul chemin
        // qui subsiste, il doit rester praticable.
        pose(LicenceInstallee.builder()
                .type("COMMERCIALE")
                .reference("LIC-2026-0001")
                .partenaireNom("CHU du Burkina Faso")
                .partenaireCode("CHU-BF")
                .jeton(LICENCE_DU_CHU)
                .debut(LocalDate.of(2026, 8, 8))
                .fin(LocalDate.of(2027, 8, 8))
                .modules("DOCUMENTAIRE,NON_CONFORMITE")
                .utilisateursMax(25)
                );

        EtatLicenceDto etat = service.etat();

        assertThat(etat.getStatut()).isEqualTo("ACTIVE");
        assertThat(etat.isActionsOuvertes()).isTrue();
        assertThat(etat.getModules()).containsExactlyInAnyOrder("DOCUMENTAIRE", "NON_CONFORMITE");
    }

    private void pose(LicenceInstallee.LicenceInstalleeBuilder licence) {
        when(repository.findTopByOrderByInstalleeLeDesc()).thenReturn(Optional.of(
                licence.installeeLe(LocalDateTime.now()).dernierJourVu(LocalDate.now()).build()));
    }
}
