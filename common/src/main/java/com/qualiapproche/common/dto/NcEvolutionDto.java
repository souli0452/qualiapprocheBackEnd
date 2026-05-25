package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NcEvolutionDto {
    private long totalEvolution;
    private String pourcentageEvolution;
    private List<GravityCountDto> gravites;
    private ChartDataDto chartData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GravityCountDto {
        private String nom;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataDto {
        private List<String> labels;
        private List<DatasetDto> datasets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetDto {
        private String label;
        private List<Long> data;
    }
}
