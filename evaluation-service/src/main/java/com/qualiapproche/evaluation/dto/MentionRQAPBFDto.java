package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Ce que le score global RQAP-BF vaut, selon la grille de la direction : niveau, mention, étoiles,
 * et les deux garde-fous qui ont pu la retenir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class MentionRQAPBFDto {
    private int rang;
    private String niveau;
    private String mention;
    private int etoiles;
    /** Les bornes de la tranche retenue : [min, max[ — max vaut 100, inclus, pour la dernière. */
    private double scoreMin;
    private double scoreMax;
    /** Le score aurait donné une tranche plus haute, mais un domaine reste sous le seuil. */
    private boolean plafonnee;
    /** Les domaines sous le seuil qui plafonnent la mention, « code — libellé ». */
    private java.util.List<String> domainesSousLeSeuil;
    /** Des écarts relevés par l'évaluation attendent d'être levés au module Non-conformités. */
    private boolean sousReserve;
    private int ecarts;
}
