package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** La part rédigée du rapport d'audit : ce que les constats ne disent pas d'eux-mêmes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedactionRapportDto {
    private String conclusions;
    private String recommandations;
}
