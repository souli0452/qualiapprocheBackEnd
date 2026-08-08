package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.entities.mappers.StructureMapper;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.referentiel.service.impl.StructureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le code partenaire appartient à l'installation, jamais au formulaire.
 *
 * <p>Il n'entre par aucun DTO et n'en ressort pas : l'écran ne le voit ni ne le pose. C'est le
 * service qui le donne, à la création comme à la modification — sans quoi deux angles morts
 * s'ouvriraient.</p>
 *
 * <p>Le premier à la <b>création</b> : une structure sans code ne se confronterait à aucune
 * licence. Le second à la <b>modification</b> : l'enregistrement reconstruit l'entité depuis le
 * DTO, et une colonne absente du DTO serait écrite à nul — renommer une direction depuis l'écran
 * aurait effacé le code, éteignant le contrôle du destinataire sans que rien ne le dise.</p>
 */
class StructureHeriteDuCodePartenaireTest {

    private StructureRepository structureRepository;
    private CodeDeLInstallation installation;
    private StructureServiceImpl service;

    @BeforeEach
    void setUp() {
        structureRepository = mock(StructureRepository.class);
        installation = new CodeDeLInstallation(structureRepository);
        ReflectionTestUtils.setField(installation, "duDeploiement", "DQA");

        StructureMapper mapper = mock(StructureMapper.class);
        when(mapper.toEntity(any(StructureDto.class))).thenAnswer(appel -> {
            StructureDto dto = appel.getArgument(0);
            Structure structure = new Structure();
            structure.setId(dto.getId());
            structure.setLibelleLong(dto.getLibelleLong());
            structure.setTypeStructure(TypeStructure.SERVICE);
            return structure;
        });
        when(mapper.toDto(any(Structure.class))).thenReturn(new StructureDto());

        service = new StructureServiceImpl(structureRepository, mapper,
                mock(LicenceInstalleeService.class), installation);

        when(structureRepository.save(any(Structure.class)))
                .thenAnswer(appel -> appel.getArgument(0));
    }

    private Structure enregistree() {
        ArgumentCaptor<Structure> capture = ArgumentCaptor.forClass(Structure.class);
        verify(structureRepository).save(capture.capture());
        return capture.getValue();
    }

    @Test
    @DisplayName("Une structure créée hérite du code de l'installation, obfusqué")
    void creation_heriteDuCode() {
        StructureDto demande = new StructureDto();
        demande.setLibelleLong("Service Qualité");

        service.saveStructure(demande);

        String pose = enregistree().getCodePartenaire();
        assertThat(pose).isNotBlank();
        // Rangé obfusqué, et non en clair : c'est ce que la colonne doit contenir.
        assertThat(pose).isNotEqualTo("DQA");
        assertThat(CryptoUtils.decrypt(pose)).isEqualTo("DQA");
    }

    @Test
    @DisplayName("Une modification ne peut pas effacer le code, que l'écran ignore")
    void modification_neffacePasLeCode() {
        UUID id = UUID.randomUUID();
        Structure existante = new Structure();
        existante.setId(id);
        existante.setCodePartenaire(CryptoUtils.encrypt("DQA"));
        when(structureRepository.findById(id)).thenReturn(Optional.of(existante));

        StructureDto demande = new StructureDto();
        demande.setId(id);
        demande.setLibelleLong("Service Qualité renommé");

        service.saveStructure(demande);

        assertThat(CryptoUtils.decrypt(enregistree().getCodePartenaire())).isEqualTo("DQA");
    }

    @Test
    @DisplayName("Sans code déclaré, rien n'est posé — et rien n'est inventé")
    void creation_sansCodeDeclare() {
        ReflectionTestUtils.setField(installation, "duDeploiement", "");
        ReflectionTestUtils.setField(installation, "attendu", null);
        when(structureRepository.findAllByTypeStructure(TypeStructure.DIRECTION))
                .thenReturn(List.of());

        StructureDto demande = new StructureDto();
        demande.setLibelleLong("Service Qualité");

        service.saveStructure(demande);

        assertThat(enregistree().getCodePartenaire()).isNull();
    }
}
