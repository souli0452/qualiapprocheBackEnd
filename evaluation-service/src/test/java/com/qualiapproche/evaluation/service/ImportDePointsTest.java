package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.PointControleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Les points d'une grille Excel ou CSV (recette du 17 oct. 2026, observation 33). */
class ImportDePointsTest {

    private final ImportDePoints importDePoints = new ImportDePoints();

    @Test
    @DisplayName("Le modèle Excel se relit : sa ligne d'exemple donne un point")
    void le_modele_se_relit() {
        List<PointControleDto> points = importDePoints.lire(new MockMultipartFile("fichier", "modele.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", importDePoints.modele()));

        assertThat(points).singleElement().satisfies(p -> {
            assertThat(p.getChapitreISO()).isEqualTo("7.5.3");
            assertThat(p.getLibelle()).isEqualTo("Maîtrise des informations documentées");
            assertThat(p.getPreuveAttendue()).startsWith("Liste des documents");
        });
    }

    @Test
    @DisplayName("Un CSV d'Excel français (point-virgule, guillemets) se lit, et une ligne sans libellé est ignorée")
    void un_csv_se_lit() {
        String csv = "﻿Chapitre;Libellé;Question;Preuve\n"
                + "8.4;\"Achats ; prestataires\";Comment sont-ils évalués ?;\"Grille \"\"A\"\"\"\n"
                + "9.1;;sans libellé;\n";
        List<PointControleDto> points = importDePoints.lire(new MockMultipartFile("fichier", "grille.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(points).singleElement().satisfies(p -> {
            assertThat(p.getLibelle()).isEqualTo("Achats ; prestataires");
            assertThat(p.getPreuveAttendue()).isEqualTo("Grille \"A\"");
        });
    }

    @Test
    @DisplayName("Un format inconnu est refusé en le disant")
    void un_format_inconnu_est_refuse() {
        assertThatThrownBy(() -> importDePoints.lire(new MockMultipartFile("fichier", "grille.pdf", "application/pdf", new byte[]{1})))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Excel");
    }
}
