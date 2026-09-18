package com.qualiapproche.ia.dto;

import com.qualiapproche.ia.enumeration.QuestionPredefinie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Une question prédéfinie, telle que l'écran la propose. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {

    private QuestionPredefinie code;
    private String libelle;
    private String description;
}
