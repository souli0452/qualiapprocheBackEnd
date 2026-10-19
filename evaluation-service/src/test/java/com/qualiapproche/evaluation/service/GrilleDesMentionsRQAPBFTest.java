package com.qualiapproche.evaluation.service;

import com.qualiapproche.evaluation.dto.MentionRQAPBFDto;
import com.qualiapproche.evaluation.entities.TrancheScoreRQAPBF;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** La grille des mentions RQAP-BF et ses garde-fous (recette du 17 oct. 2026, observation 28). */
class GrilleDesMentionsRQAPBFTest {

    private final List<TrancheScoreRQAPBF> grille = GrilleDesMentionsRQAPBF.grilleParDefaut();

    private MentionRQAPBFDto mention(double score, Map<String, Double> domaines, int ecarts) {
        return GrilleDesMentionsRQAPBF.mention(grille, score, domaines, ecarts);
    }

    @Test
    @DisplayName("La seule tranche connue de la grille officielle : [50–70[ = intermédiaire, deux étoiles")
    void la_tranche_connue() {
        MentionRQAPBFDto m = mention(62, Map.of(), 0);
        assertThat(m.getNiveau()).isEqualTo("Niveau intermédiaire");
        assertThat(m.getMention()).isEqualTo("Certificat de conformité moyenne");
        assertThat(m.getEtoiles()).isEqualTo(2);
        assertThat(m.getScoreMin()).isEqualTo(50);
        assertThat(m.getScoreMax()).isEqualTo(70);
    }

    @Test
    @DisplayName("La borne basse est incluse, sur le score arrondi à une décimale")
    void bornes() {
        assertThat(mention(70.0, Map.of(), 0).getRang()).isEqualTo(4);
        assertThat(mention(69.94, Map.of(), 0).getRang()).isEqualTo(3);
        assertThat(mention(69.96, Map.of(), 0).getRang()).isEqualTo(4);
        assertThat(mention(0, Map.of(), 0).getRang()).isEqualTo(1);
        assertThat(mention(100, Map.of(), 0).getRang()).isEqualTo(5);
        assertThat(GrilleDesMentionsRQAPBF.mention(grille, null, Map.of(), 0)).isNull();
    }

    @Test
    @DisplayName("Un domaine sous 50 % plafonne la mention à l'intermédiaire, et le dit")
    void plancher_par_domaine() {
        Map<String, Double> domaines = new LinkedHashMap<>();
        domaines.put("D1 — Gouvernance", 98.0);
        domaines.put("D2 — Ressources", 40.0);
        MentionRQAPBFDto m = mention(92, domaines, 0);
        assertThat(m.getRang()).isEqualTo(3);
        assertThat(m.isPlafonnee()).isTrue();
        assertThat(m.getDomainesSousLeSeuil()).containsExactly("D2 — Ressources");
    }

    @Test
    @DisplayName("Le plancher ne remonte pas une mention déjà plus basse")
    void plancher_sans_effet_en_dessous() {
        MentionRQAPBFDto m = mention(35, Map.of("D2 — Ressources", 20.0), 0);
        assertThat(m.getRang()).isEqualTo(2);
        assertThat(m.isPlafonnee()).isFalse();
    }

    @Test
    @DisplayName("Des écarts relevés : la mention est délivrée sous réserve")
    void reserve() {
        assertThat(mention(80, Map.of(), 3)).satisfies(m -> {
            assertThat(m.isSousReserve()).isTrue();
            assertThat(m.getEcarts()).isEqualTo(3);
        });
        assertThat(mention(80, Map.of(), 0).isSousReserve()).isFalse();
    }
}
