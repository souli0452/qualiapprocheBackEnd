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
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les règles d'une structure : un nom unique, une seule direction, et la licence portée par elle
 * seule.
 *
 * <p>Le code partenaire a son propre test ({@link StructureHeriteDuCodePartenaireTest}) ; la
 * direction vue par la licence aussi ({@link DirectionSuitLaLicenceInstalleeTest}). On vérifie ici
 * ce qui empêche deux structures de se confondre, et ce que l'écran reçoit quand il en cherche
 * une.</p>
 */
class ReglesDesStructuresTest {

    private StructureRepository depot;
    private LicenceInstalleeService licence;
    private StructureServiceImpl service;

    @BeforeEach
    void preparer() {
        depot = mock(StructureRepository.class);
        licence = mock(LicenceInstalleeService.class);
        CodeDeLInstallation installation = new CodeDeLInstallation(depot);
        ReflectionTestUtils.setField(installation, "duDeploiement", "DQA");

        StructureMapper mapper = mock(StructureMapper.class);
        when(mapper.toEntity(any(StructureDto.class))).thenAnswer(appel -> {
            StructureDto dto = appel.getArgument(0);
            Structure s = new Structure();
            s.setId(dto.getId());
            s.setLibelleLong(dto.getLibelleLong());
            s.setLibelleCourt(dto.getLibelleCourt());
            s.setTypeStructure(dto.getTypeStructure() == null ? null : TypeStructure.valueOf(dto.getTypeStructure()));
            return s;
        });
        when(mapper.toDto(any(Structure.class))).thenAnswer(appel -> {
            Structure s = appel.getArgument(0);
            StructureDto dto = new StructureDto();
            dto.setId(s.getId());
            dto.setLibelleLong(s.getLibelleLong());
            dto.setLibelleCourt(s.getLibelleCourt());
            dto.setLicenceActive(s.getLicenceActive());
            return dto;
        });
        service = new StructureServiceImpl(depot, mapper, licence, installation);
        when(depot.save(any(Structure.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    private static StructureDto dto(UUID id, String libelle, TypeStructure type) {
        StructureDto d = new StructureDto();
        d.setId(id);
        d.setLibelleLong(libelle);
        d.setLibelleCourt(libelle);
        d.setTypeStructure(type.name());
        return d;
    }

    private static Structure structure(String libelleCourt, String libelleLong) {
        Structure s = new Structure();
        s.setId(UUID.randomUUID());
        s.setLibelleCourt(libelleCourt);
        s.setLibelleLong(libelleLong);
        return s;
    }

    @Test
    @DisplayName("Deux structures ne portent pas le même nom : 409 à la création comme à la modification")
    void nomUnique() {
        when(depot.existsByLibelleLong("Service qualité")).thenReturn(true);
        UUID id = UUID.randomUUID();
        when(depot.existsByLibelleLongAndIdNot("Service achats", id)).thenReturn(true);

        assertThatThrownBy(() -> service.saveStructure(dto(null, "Service qualité", TypeStructure.SERVICE)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.saveStructure(dto(id, "Service achats", TypeStructure.SERVICE)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Modifier une structure en gardant son nom n'est pas un doublon d'elle-même")
    void sonPropreNom() {
        UUID id = UUID.randomUUID();
        when(depot.existsByLibelleLongAndIdNot("Service qualité", id)).thenReturn(false);

        service.saveStructure(dto(id, "Service qualité", TypeStructure.SERVICE));

        verify(depot).save(any(Structure.class));
        verify(depot, never()).existsByLibelleLong(any());
    }

    @Test
    @DisplayName("Une seule direction : la seconde est refusée en 409")
    void uneSeuleDirection() {
        when(depot.existsByTypeStructure(TypeStructure.DIRECTION)).thenReturn(true);

        assertThatThrownBy(() -> service.saveStructure(dto(null, "Autre direction", TypeStructure.DIRECTION)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getReason()).contains("DIRECTION");
                });
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("La direction existante se modifie : elle ne se compte pas contre elle-même")
    void directionModifiee() {
        UUID id = UUID.randomUUID();
        when(depot.existsByTypeStructureAndIdNot(TypeStructure.DIRECTION, id)).thenReturn(false);

        service.saveStructure(dto(id, "Direction générale", TypeStructure.DIRECTION));

        verify(depot).save(any(Structure.class));
    }

    @Test
    @DisplayName("À la création, seule la direction porte une licence active")
    void licenceALaCreation() {
        ArgumentCaptor<Structure> enregistree = ArgumentCaptor.forClass(Structure.class);

        service.saveStructure(dto(null, "Direction générale", TypeStructure.DIRECTION));
        service.saveStructure(dto(null, "Service qualité", TypeStructure.SERVICE));

        verify(depot, org.mockito.Mockito.times(2)).save(enregistree.capture());
        assertThat(enregistree.getAllValues()).extracting(Structure::getLicenceActive).containsExactly(true, false);
    }

    @Test
    @DisplayName("Modifier ou supprimer une structure inconnue : 400, rien n'est écrit")
    void structureInconnue() {
        UUID inconnue = UUID.randomUUID();
        when(depot.existsById(inconnue)).thenReturn(false);

        assertThatThrownBy(() -> service.updateStructure(dto(inconnue, "X", TypeStructure.SERVICE)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.updateStructure(dto(null, "X", TypeStructure.SERVICE)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.deleteStructure(inconnue))
                .isInstanceOf(ResponseStatusException.class);
        verify(depot, never()).save(any());
        verify(depot, never()).deleteById(any());
    }

    @Test
    @DisplayName("Une structure introuvable par son identifiant : 404")
    void introuvable() {
        UUID id = UUID.randomUUID();
        when(depot.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStructureById(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.getStructureNameById(id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Par libellé : le sigle d'abord ; s'il est ambigu, le nom complet tranche")
    void parLibelle() {
        Structure qualite = structure("SQ", "Service qualité");
        when(depot.findByLibelleCourt("SQ")).thenReturn(List.of(qualite));
        Structure achats = structure("SA", "Service achats");
        when(depot.findByLibelleCourt("Service achats")).thenReturn(List.of(achats, structure("SA", "Autre")));
        when(depot.findByLibelleLong("Service achats")).thenReturn(Optional.of(achats));

        assertThat(service.findStructureByLibelle("SQ").getId()).isEqualTo(qualite.getId());
        assertThat(service.findStructureByLibelle("Service achats").getId()).isEqualTo(achats.getId());
    }

    @Test
    @DisplayName("Par libellé, rien ou une ambiguïté que le nom complet ne lève pas : 404")
    void parLibelleIntrouvable() {
        when(depot.findByLibelleCourt("X")).thenReturn(List.of());
        when(depot.findByLibelleCourt("SA")).thenReturn(List.of(structure("SA", "A"), structure("SA", "B")));
        when(depot.findByLibelleLong("SA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findStructureByLibelle("X"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.findStructureByLibelle("SA"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("La liste se trie par nom faute d'ordre demandé ; l'ordre demandé est respecté")
    @SuppressWarnings("unchecked")
    void ordreDeLaListe() {
        when(depot.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);

        service.getAllStructures(TypeStructure.SERVICE, UUID.randomUUID(), " qual ", PageRequest.of(2, 20));
        service.getAllStructures(null, null, null, PageRequest.of(0, 5, Sort.by("libelleCourt")));

        verify(depot, org.mockito.Mockito.times(2)).findAll(any(Specification.class), page.capture());
        assertThat(page.getAllValues().get(0).getSort()).isEqualTo(Sort.by("libelleLong"));
        assertThat(page.getAllValues().get(0).getPageNumber()).isEqualTo(2);
        assertThat(page.getAllValues().get(1).getSort()).isEqualTo(Sort.by("libelleCourt"));
    }

    @Test
    @DisplayName("Sans licence posée, la direction annonce zéro jour et aucun module, pas un compte à rebours")
    void directionSansLicence() {
        Structure direction = structure("DG", "Direction générale");
        when(depot.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of(direction));
        when(licence.etat()).thenReturn(EtatLicenceDto.builder().statut("ABSENTE").actionsOuvertes(false)
                .joursRestants(-400).modules(List.of()).build());

        StructureDto dto = service.getDirection();

        assertThat(dto.getLicenceActive()).isFalse();
        assertThat(dto.getLicenseDaysRemaining()).isZero();
        assertThat(dto.getDateFinLicence()).isNull();
    }

    @Test
    @DisplayName("L'état de licence d'une structure est celui de la direction ; sans direction, tout est fermé")
    void etatDeLicenceDUneStructure() {
        Structure direction = structure("DG", "Direction générale");
        when(depot.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of(direction));
        when(licence.etat()).thenReturn(EtatLicenceDto.builder().statut("ACTIVE").actionsOuvertes(true)
                .joursRestants(12).fin(LocalDate.now().plusDays(12)).modules(List.of("AUDIT")).build());

        Map<String, Object> etat = service.getStructureLicenseStatus(UUID.randomUUID());
        assertThat(etat).containsEntry("licenseActive", true).containsEntry("daysRemaining", 12)
                .containsEntry("modules", List.of("AUDIT"));

        when(depot.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of());
        assertThat(service.getStructureLicenseStatus(UUID.randomUUID()))
                .containsEntry("licenseActive", false).containsEntry("daysRemaining", 0)
                .containsEntry("modules", List.of());
    }
}
