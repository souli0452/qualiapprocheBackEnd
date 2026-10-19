package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class EvaluationRQAPBFDto {
    private UUID id;
    private UUID auditId;
    private LocalDate dateEvaluation;
    private Double scoreGlobal;
    private String niveauConformite;
    private StatutEvaluation statut;
    private List<NotationCritereDto> notations;
    /** Prévisualisation : sur tous les critères notés, publiés ou non. */
    private Map<String, Double> resultatsParDomaine;
    /** Résultats officiels : sur les seuls critères publiés, comme le rapport. */
    private Double scoreOfficiel;
    private Map<String, Double> resultatsOfficielsParDomaine;
    /** Ce que vaut le score selon la grille des mentions : provisoire, puis officielle. */
    private MentionRQAPBFDto mention;
    private MentionRQAPBFDto mentionOfficielle;
    /** Prévisualisation, par code de nœud : le score de chaque section, exigence... */
    private Map<String, Double> scoresParNoeud;
    private int criteresNotes;
    private int criteresPublies;
}
