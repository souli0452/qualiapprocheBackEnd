package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.entities.mappers.StructureMapper;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.referentiel.service.impl.StructureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce que les écrans annoncent vient de la licence installée, et de rien d'autre.
 *
 * <p>C'est {@code /structures/direction} qui décide des menus affichés et du bandeau de licence,
 * jusque dans la réponse de connexion. Ces champs étaient lus dans {@code abonnements_directions},
 * remplie au démarrage depuis un fichier du produit — donc sans rapport avec la licence que la
 * passerelle, elle, vérifie réellement. Une installation pouvait ainsi montrer deux modules
 * ouverts jusqu'en 2027 et refuser chaque écriture en 402 : l'utilisateur voyait un module qu'il
 * ne pouvait pas utiliser, sans explication cohérente.</p>
 *
 * <p>Ces trois cas fixent l'accord entre ce qui est montré et ce qui est permis.</p>
 */
class DirectionSuitLaLicenceInstalleeTest {

    private StructureRepository structureRepository;
    private LicenceInstalleeService licenceInstalleeService;
    private StructureServiceImpl service;

    @BeforeEach
    void setUp() {
        structureRepository = mock(StructureRepository.class);
        licenceInstalleeService = mock(LicenceInstalleeService.class);
        StructureMapper mapper = mock(StructureMapper.class);
        when(mapper.toDto(any(Structure.class))).thenAnswer(appel -> {
            Structure structure = appel.getArgument(0);
            StructureDto dto = new StructureDto();
            dto.setLibelleLong(structure.getLibelleLong());
            return dto;
        });

        service = new StructureServiceImpl(structureRepository, mapper, licenceInstalleeService,
                new CodeDeLInstallation(structureRepository));

        when(structureRepository.findAllByTypeStructure(TypeStructure.DIRECTION))
                .thenReturn(List.of(Structure.builder()
                        .libelleLong("Direction Qualité Approche")
                        .typeStructure(TypeStructure.DIRECTION)
                        .build()));
    }

    @Test
    @DisplayName("Licence active : les modules souscrits, et eux seuls, sont annoncés")
    void licenceActive() {
        when(licenceInstalleeService.etat()).thenReturn(EtatLicenceDto.builder()
                .statut("ACTIVE")
                .actionsOuvertes(true)
                .debut(LocalDate.of(2026, 1, 1))
                .fin(LocalDate.of(2027, 1, 1))
                .joursRestants(146)
                .modules(List.of("NON_CONFORMITE"))
                .build());

        StructureDto direction = service.getDirection();

        assertThat(direction.getLibelleLong()).isEqualTo("Direction Qualité Approche");
        assertThat(direction.getLicenceActive()).isTrue();
        assertThat(direction.getModulesSubscribed()).containsExactly("NON_CONFORMITE");
        assertThat(direction.getLicenseDaysRemaining()).isEqualTo(146L);
        assertThat(direction.getDateFinLicence()).isEqualTo(LocalDate.of(2027, 1, 1).atStartOfDay());
    }

    @Test
    @DisplayName("Licence expirée : les modules restent annoncés, les actions non")
    void licenceExpiree() {
        when(licenceInstalleeService.etat()).thenReturn(EtatLicenceDto.builder()
                .statut("EXPIREE")
                .actionsOuvertes(false)
                .debut(LocalDate.of(2024, 1, 1))
                .fin(LocalDate.of(2025, 1, 1))
                .joursRestants(-584)
                .modules(List.of("NON_CONFORMITE", "DOCUMENTAIRE"))
                .build());

        StructureDto direction = service.getDirection();

        // Les menus restent en place : couper l'accès aux données qualité d'un client
        // transformerait un retard de paiement en litige. Seules les écritures sont suspendues,
        // et c'est la passerelle qui les refuse.
        assertThat(direction.getModulesSubscribed()).containsExactly("NON_CONFORMITE", "DOCUMENTAIRE");
        assertThat(direction.getLicenceActive()).isFalse();
        assertThat(direction.getLicenseDaysRemaining()).isEqualTo(-584L);
    }

    @Test
    @DisplayName("Aucune licence : rien n'est annoncé comme ouvert")
    void licenceAbsente() {
        when(licenceInstalleeService.etat()).thenReturn(EtatLicenceDto.builder()
                .statut("ABSENTE")
                .actionsOuvertes(false)
                .modules(List.of())
                .build());

        StructureDto direction = service.getDirection();

        // Le cas que l'ancien dispositif ne pouvait pas produire : le fichier d'amorçage accordait
        // toujours une licence, valide et généreuse, à toute installation neuve.
        assertThat(direction.getLicenceActive()).isFalse();
        assertThat(direction.getModulesSubscribed()).isEmpty();
        assertThat(direction.getLicenseDaysRemaining()).isZero();
        assertThat(direction.getDateDebutLicence()).isNull();
        assertThat(direction.getDateFinLicence()).isNull();
    }

    @Test
    @DisplayName("Aucune direction en base : pas de licence inventée")
    void sansDirection() {
        when(structureRepository.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of());

        assertThat(service.getDirection()).isNull();
    }
}
