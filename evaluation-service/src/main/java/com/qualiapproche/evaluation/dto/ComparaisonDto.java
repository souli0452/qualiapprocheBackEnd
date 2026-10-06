package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** L'historique comparé de deux périodes (D17) : maturité RQAP-BF et écarts par chapitre ISO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparaisonDto {

    private Periode periode1;
    private Periode periode2;
    private List<EcartsParChapitre> ecartsParChapitre;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Periode {
        private LocalDate debut;
        private LocalDate fin;
        private int evaluations;
        private Double scoreMoyen;
        private Map<String, Double> parDomaine;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EcartsParChapitre {
        private String chapitre;
        private long periode1;
        private long periode2;
        /** EN_AMELIORATION si moins d'écarts, EN_DEGRADATION si plus, STABLE sinon. */
        private String evolution;
    }
}
