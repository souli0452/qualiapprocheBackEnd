package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "L'évolution des non-conformités sur une année, ou sur un mois de cette "
        + "année, avec de quoi tracer la courbe sans autre appel. L'année, le mois et la "
        + "structure retenus viennent des paramètres de la requête et ne sont pas repris ici.")
public class NcEvolutionDto {
    @Schema(description = "Nombre de dossiers déclarés sur la période demandée.",
            example = "87")
    private long totalEvolution;

    @Schema(description = "Écart avec la période qui précède — l'année d'avant, ou le mois "
            + "d'avant. Chaîne et non nombre : signe compris, une décimale, point pour "
            + "séparateur, sans le signe pourcent. Vaut « 100.0 » quand la période précédente "
            + "était vide, faute de base de comparaison.",
            example = "+12.5")
    private String pourcentageEvolution;

    @Schema(description = "Total par niveau de gravité sur la période. Tous les niveaux du "
            + "référentiel y figurent, y compris ceux restés à zéro.")
    private List<GravityCountDto> gravites;

    @Schema(description = "Les mêmes chiffres disposés pour le graphique : une abscisse par pas "
            + "de temps, une série par niveau de gravité.")
    private ChartDataDto chartData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Le total d'un niveau de gravité sur toute la période, tous pas de "
            + "temps confondus.")
    public static class GravityCountDto {
        @Schema(description = "Libellé du niveau, tel que le référentiel le nomme.",
                example = "Majeure")
        private String nom;

        @Schema(description = "Nombre de dossiers de ce niveau sur la période.",
                example = "23")
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Les abscisses et les séries du graphique, prêtes à être tracées telles "
            + "quelles.")
    public static class ChartDataDto {
        @Schema(description = "Abscisses : les douze mois abrégés quand la demande porte sur une "
                + "année, les quatre semaines quand elle porte sur un mois — la cinquième semaine "
                + "entamée étant repliée sur la quatrième.")
        private List<String> labels;

        @Schema(description = "Une série par niveau de gravité du référentiel, toutes de la même "
                + "longueur que les abscisses.")
        private List<DatasetDto> datasets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Une série du graphique : un niveau de gravité suivi sur toute la "
            + "période.")
    public static class DatasetDto {
        @Schema(description = "Libellé du niveau de gravité que la série suit.",
                example = "Mineure")
        private String label;

        @Schema(description = "Valeurs de la série, dans l'ordre des abscisses et de même "
                + "longueur qu'elles ; les pas de temps sans dossier valent zéro.")
        private List<Long> data;
    }
}
