package com.qualiapproche.referentiel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.referentiel.entities.Structure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le code partenaire ne sort par aucune API.
 *
 * <p>Il n'accorde aucun droit : il désigne seulement à qui la licence doit correspondre. Mais
 * c'est précisément la valeur qu'il faut connaître pour faire passer la licence d'un autre client
 * — la publier en consultation ou en liste apprendrait à qui sait lire une réponse JSON la chaîne
 * exacte à recopier dans son propre {@code tenant-init.json}.</p>
 *
 * <p>Deux portes, et deux gardes. L'entité voyage telle quelle dans certaines réponses, d'où le
 * {@code @JsonIgnore} ; le DTO, lui, est sérialisé intégralement, et c'est en lui ajoutant un
 * champ qu'on ouvrirait la fuite sans y penser. Aucune des deux ne se voit à la relecture d'une
 * ligne de diff — d'où ce test.</p>
 */
class CodePartenaireNeSortPasTest {

    private static final String CODE = "CHU-BF";

    @Test
    @DisplayName("Sérialisée, la structure ne laisse rien voir de son code")
    void entite_neSerialisePasLeCode() throws Exception {
        Structure direction = Structure.builder()
                .libelleLong("Direction Qualité Approche")
                .libelleCourt("DQA")
                .typeStructure(TypeStructure.DIRECTION)
                .codePartenaire(CODE)
                .build();

        String json = new ObjectMapper().writeValueAsString(direction);

        assertThat(json).doesNotContain(CODE).doesNotContain("codePartenaire");
        // Le reste passe : c'est bien la structure qui est sérialisée, pas un objet vide.
        assertThat(json).contains("Direction Qualité Approche");
    }

    @Test
    @DisplayName("Le DTO ne porte aucun champ de code : l'ajouter serait la fuite")
    void dto_neTransportePasLeCode() {
        // Le DTO n'a pas de @JsonIgnore à oublier : il n'a simplement pas le champ. Ce test garde
        // cette absence, qui est la seule protection réelle du côté sortie.
        assertThat(Arrays.stream(StructureDto.class.getDeclaredFields()).map(Field::getName))
                .noneMatch(nom -> nom.toLowerCase().contains("codepartenaire"))
                .noneMatch(nom -> nom.equalsIgnoreCase("code"));
    }

    @Test
    @DisplayName("Le DTO sérialisé ne contient pas le code, même à travers l'héritage")
    void dtoSerialise_neContientPasLeCode() throws Exception {
        StructureDto dto = new StructureDto();
        dto.setLibelleLong("Direction Qualité Approche");
        dto.setLibelleCourt("DQA");

        assertThat(new ObjectMapper().writeValueAsString(dto)).doesNotContain(CODE);
    }
}
