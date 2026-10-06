package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le tableau de bord du module (A1) et les indicateurs du suivi du programme (D16), pour une année.
 *
 * <p>« Planifiés » compte les audits de l'année qui ne sont pas annulés : un audit annulé n'est
 * pas un audit qu'on a manqué de réaliser.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableauDeBordDto {

    private int annee;
    private long auditsPlanifies;
    private long auditsRealises;
    /** Réalisés sur planifiés, en pourcentage ; nul s'il n'y a rien de planifié. */
    private Double tauxRealisation;
    private long auditsEnRetard;
    /** Constats publiés générant une NC, et critères RQAP-BF publiés au constat NC. */
    private long ecartsDetectes;
    /** Moyenne des scores de maturité des audits de l'année qui en ont un. */
    private Double scoreMaturiteMoyen;
    /** Radar RQAP-BF : score officiel moyen par domaine, sur les évaluations de l'année. */
    private Map<String, Double> radarParDomaine;
    private List<Rappel> rappelsRecents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rappel {
        private UUID auditId;
        private String auditReference;
        private String regle;
        private int delaiJours;
        private String destinataires;
        private LocalDate dateRappel;
    }
}
