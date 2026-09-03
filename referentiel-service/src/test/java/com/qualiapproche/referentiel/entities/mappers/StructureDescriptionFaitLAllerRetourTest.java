package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.referentiel.entities.Structure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La description d'une structure doit survivre au trajet écran → base → écran.
 *
 * <p>Le champ figurait dans le DTO depuis l'origine sans contrepartie dans l'entité. MapStruct ne
 * s'en plaint pas : une propriété source sans cible est ignorée, une cible sans source reste
 * nulle. La saisie était donc perdue sans erreur, et le formulaire de modification d'un service
 * revenait toujours vide à cet endroit. Ce test passe par le mapper généré, et non par un
 * simulacre, parce que c'est précisément le mapping qui était en cause.</p>
 */
class StructureDescriptionFaitLAllerRetourTest {

    private final StructureMapper mapper = new StructureMapperImpl();

    @Test
    @DisplayName("La description saisie est portée jusqu'à l'entité")
    void versLEntite() {
        StructureDto dto = new StructureDto();
        dto.setLibelleLong("Service Qualité");
        dto.setDescription("Pilotage du système de management de la qualité.");

        Structure entite = mapper.toEntity(dto);

        assertThat(entite.getDescription())
                .isEqualTo("Pilotage du système de management de la qualité.");
    }

    @Test
    @DisplayName("La description enregistrée revient dans le DTO relu")
    void versLeDto() {
        Structure entite = new Structure();
        entite.setLibelleLong("Service Qualité");
        entite.setDescription("Pilotage du système de management de la qualité.");

        StructureDto dto = mapper.toDto(entite);

        assertThat(dto.getDescription())
                .isEqualTo("Pilotage du système de management de la qualité.");
    }
}
