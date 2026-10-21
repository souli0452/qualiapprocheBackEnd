package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Le bandeau de la compilation d'équipe : combien de constats, où ils en sont, et par nature.
 *
 * <p>Les groupes suivent l'ordre du paramétrage des natures, et seules les natures représentées
 * y figurent — comme les blocs de l'écran de compilation.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {

    private long total;
    private long compiles;
    private long publies;
    private List<Groupe> parNature;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Groupe {
        private UUID natureId;
        private String libelle;
        private String couleur;
        private boolean genereNonConformite;
        private long nombre;
        /** Ce qui reste à valider dans ce groupe : 0 affiche « Tout validé ». */
        private long compiles;
    }
}
